import asyncio
import time
import uuid

import torch

from app.batching.enums import BatchTrigger, BatchType, RequestState
from app.batching.models import BatchTask
from app.log_utils import get_model_service_logger


logger = get_model_service_logger()


class ImageBatchProcessor:
    """收集已预处理图片 Tensor，并向 GPU 队列提交图片批次。"""

    def __init__(self, ready_queue, gpu_batch_queue, executors, properties, result_builder, stop_event):
        """初始化图片批处理器。

        Args:
            ready_queue: 图片预处理完成队列。
            gpu_batch_queue: 统一 GPU 批任务队列。
            executors: CPU/GPU 线程池容器。
            properties: 图片批处理配置。
            result_builder: 用于构造失败结果的服务门面。
            stop_event: 服务关闭事件。

        Returns:
            None。
        """

        self.ready_queue = ready_queue
        self.gpu_batch_queue = gpu_batch_queue
        self.executors = executors
        self.properties = properties
        self.result_builder = result_builder
        self.stop_event = stop_event

    async def run(self) -> None:
        """持续收集、stack 并提交图片批次。

        Args:
            None。

        Returns:
            None。
        """

        while not self.stop_event.is_set():
            collected_items = []
            active_items = []
            try:
                collected_items, trigger = await self._collect_batch()
                active_items = self._filter_active(collected_items)
                if not active_items:
                    continue
                tensor = await self._stack_batch(active_items)
                batch = self._build_batch_task(active_items, tensor, trigger)
                await self._submit_batch(batch)
            except asyncio.CancelledError:
                self._fail_items(
                    active_items or collected_items,
                    "SERVICE_SHUTTING_DOWN",
                    "vectorization service is shutting down",
                )
                raise
            except Exception as exc:
                logger.exception(f"event=IMAGE_BATCH_PROCESS_FAILED message={str(exc)}")
                self._fail_items(active_items or collected_items, "IMAGE_BATCH_PROCESS_ERROR", str(exc))
            finally:
                for _ in collected_items:
                    self.ready_queue.task_done()

    async def _collect_batch(self):
        """收集到最大批量，或等到首个 ready 图片的最长等待时间。

        Args:
            None。

        Returns:
            二元组 ``(items, trigger)``，分别表示收集到的图片和触发原因。
        """

        first_item = await self.ready_queue.get()
        items = [first_item]
        trigger = BatchTrigger.TIME
        deadline = first_item.ready_at + (self.properties.max_wait_ms / 1000.0)
        while len(items) < self.properties.max_batch_size:
            try:
                items.append(self.ready_queue.get_nowait())
                if len(items) == self.properties.max_batch_size:
                    trigger = BatchTrigger.SIZE
                    break
            except asyncio.QueueEmpty:
                remaining = deadline - time.perf_counter()
                if remaining <= 0:
                    break
                try:
                    items.append(await asyncio.wait_for(self.ready_queue.get(), timeout=remaining))
                except asyncio.TimeoutError:
                    break
        return items, trigger

    def _filter_active(self, items):
        """构造 batch tensor 前过滤已终态或已超时的 Ticket。

        Args:
            items: PreparedImageItem 列表。

        Returns:
            仍然可继续处理的 PreparedImageItem 列表。
        """

        now = time.perf_counter()
        active = []
        for item in items:
            if item.ticket.is_terminal():
                continue
            if item.ticket.is_expired(now):
                item.ticket.try_fail(self.result_builder.failure_result_for_ticket(item.ticket, "REQUEST_TIMEOUT", "request timeout"))
                continue
            active.append(item)
        return active

    async def _stack_batch(self, items):
        """在线程池中执行 torch.stack，构造图片批量 Tensor。

        Args:
            items: PreparedImageItem 列表。

        Returns:
            图片批量 Tensor，形状通常为 ``[B,C,H,W]``。
        """

        tensors = [item.image_tensor for item in items]
        return await self.executors.run_cpu(torch.stack, tensors)

    def _build_batch_task(self, items, tensor, trigger):
        """构造 BatchTask，并保持图片 Tensor 行与 Ticket 下标一致。

        Args:
            items: PreparedImageItem 列表。
            tensor: 图片批量 Tensor。
            trigger: 批次触发原因。

        Returns:
            图片 BatchTask。

        Raises:
            ValueError: batch tensor 大小与 Ticket 数量不一致时抛出。
        """

        tickets = tuple(item.ticket for item in items)
        if tensor.shape[0] != len(tickets):
            raise ValueError("image tensor batch size mismatch")
        now = time.perf_counter()
        return BatchTask(
            batch_id=uuid.uuid4().hex,
            batch_type=BatchType.IMAGE,
            tickets=tickets,
            input_tensor=tensor,
            trigger=trigger,
            created_at=now,
            gpu_enqueue_at=now,
        )

    async def _submit_batch(self, batch):
        """把仍然有效的图片批次提交到 GPU FIFO 队列。

        Args:
            batch: 待提交的图片 BatchTask。

        Returns:
            None。
        """

        batch = self._filter_submittable_batch(batch)
        if batch is None:
            return
        self._ensure_expected_state(batch.tickets, RequestState.READY)
        for ticket in batch.tickets:
            ticket.transition(RequestState.READY, RequestState.GPU_QUEUED)
            ticket.batch_id = batch.batch_id
        try:
            await self.gpu_batch_queue.put(batch)
        except asyncio.CancelledError:
            self._fail_tickets(
                batch.tickets,
                "SERVICE_SHUTTING_DOWN",
                "vectorization service is shutting down",
            )
            raise

    def _fail_items(self, items, error_code, message):
        """用失败结果完成一组图片请求。

        Args:
            items: PreparedImageItem 列表。
            error_code: 错误码。
            message: 错误说明。

        Returns:
            None。
        """

        for item in items:
            item.ticket.try_fail(self.result_builder.failure_result_for_ticket(item.ticket, error_code, message))

    def _fail_tickets(self, tickets, error_code, message):
        """用失败结果完成一组 Ticket。

        Args:
            tickets: RequestTicket 可迭代对象。
            error_code: 错误码。
            message: 错误说明。

        Returns:
            None。
        """

        for ticket in tickets:
            ticket.try_fail(self.result_builder.failure_result_for_ticket(ticket, error_code, message))

    def _filter_submittable_batch(self, batch):
        """返回只包含可安全入队 Ticket 的批任务。

        Args:
            batch: 原始 BatchTask。

        Returns:
            过滤后的 BatchTask；如果没有可提交 Ticket，则返回 None。
        """

        now = time.perf_counter()
        active_indices = []
        active_tickets = []
        for index, ticket in enumerate(batch.tickets):
            if ticket.is_terminal():
                continue
            if ticket.is_expired(now):
                ticket.try_fail(self.result_builder.failure_result_for_ticket(ticket, "REQUEST_TIMEOUT", "request timeout"))
                continue
            active_indices.append(index)
            active_tickets.append(ticket)
        if not active_tickets:
            return None
        if len(active_tickets) == len(batch.tickets):
            return batch
        return BatchTask(
            batch_id=batch.batch_id,
            batch_type=batch.batch_type,
            tickets=tuple(active_tickets),
            input_tensor=batch.input_tensor[active_indices],
            trigger=batch.trigger,
            created_at=batch.created_at,
            gpu_enqueue_at=time.perf_counter(),
        )

    @staticmethod
    def _ensure_expected_state(tickets, expected_state):
        """校验提交前所有 Ticket 都处于指定状态。

        Args:
            tickets: RequestTicket 可迭代对象。
            expected_state: 期望状态。

        Returns:
            None。

        Raises:
            ValueError: 任意 Ticket 状态不符合预期时抛出。
        """

        for ticket in tickets:
            if ticket.state != expected_state:
                raise ValueError(f"ticket state mismatch before submit: expected={expected_state} actual={ticket.state}")
