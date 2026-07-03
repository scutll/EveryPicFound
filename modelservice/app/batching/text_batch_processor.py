import asyncio
import time
import uuid

from app.batching.enums import BatchTrigger, BatchType, RequestState
from app.batching.models import BatchTask
from app.log_utils import get_model_service_logger


logger = get_model_service_logger()


class TextBatchProcessor:
    """收集文本请求，并向 GPU 队列提交 token 批次。

    该协程是 ``TextPendingQueue`` 的唯一消费者。Tokenizer 提交给 CPU executor，
    避免阻塞事件循环处理新的 HTTP 请求和其他后台协程。
    """

    def __init__(self, pending_queue, gpu_batch_queue, executors, runtime, properties, result_builder, stop_event):
        """初始化文本批处理器。

        Args:
            pending_queue: 文本待处理队列。
            gpu_batch_queue: 统一 GPU 批任务队列。
            executors: CPU/GPU 线程池容器。
            runtime: OpenCLIP 运行时对象。
            properties: 文本批处理配置。
            result_builder: 用于构造成功/失败结果的服务门面。
            stop_event: 服务关闭事件。

        Returns:
            None。
        """

        self.pending_queue = pending_queue
        self.gpu_batch_queue = gpu_batch_queue
        self.executors = executors
        self.runtime = runtime
        self.properties = properties
        self.result_builder = result_builder
        self.stop_event = stop_event

    async def run(self) -> None:
        """持续收集、Tokenize 并提交文本批次。

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
                tokens = await self._tokenize_batch(active_items)
                batch = self._build_batch_task(active_items, tokens, trigger)
                await self._submit_batch(batch)
            except asyncio.CancelledError:
                self._fail_items(
                    active_items or collected_items,
                    "SERVICE_SHUTTING_DOWN",
                    "vectorization service is shutting down",
                )
                raise
            except Exception as exc:
                logger.exception(f"event=TEXT_BATCH_PROCESS_FAILED message={str(exc)}")
                self._fail_items(active_items or collected_items, "TEXT_BATCH_PROCESS_ERROR", str(exc))
            finally:
                for _ in collected_items:
                    self.pending_queue.task_done()

    async def _collect_batch(self):
        """收集到最大批量，或等到首个请求的最长等待时间。

        Args:
            None。

        Returns:
            二元组 ``(items, trigger)``，分别表示收集到的请求和触发原因。
        """

        first_item = await self.pending_queue.get()
        items = [first_item]
        trigger = BatchTrigger.TIME
        deadline = first_item.enqueue_at + (self.properties.max_wait_ms / 1000.0)

        while len(items) < self.properties.max_batch_size:
            try:
                items.append(self.pending_queue.get_nowait())
                if len(items) == self.properties.max_batch_size:
                    trigger = BatchTrigger.SIZE
                    break
            except asyncio.QueueEmpty:
                remaining = deadline - time.perf_counter()
                if remaining <= 0:
                    break
                try:
                    items.append(await asyncio.wait_for(self.pending_queue.get(), timeout=remaining))
                except asyncio.TimeoutError:
                    break
        return items, trigger

    def _filter_active(self, items):
        """CPU Tokenizer 前过滤已终态或已超时的 Ticket。

        Args:
            items: 从文本队列收集到的 TextPendingItem 列表。

        Returns:
            仍然可继续处理的 TextPendingItem 列表。
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

    async def _tokenize_batch(self, items):
        """在线程池中执行批量 Tokenizer。

        Args:
            items: 需要 Tokenize 的 TextPendingItem 列表。

        Returns:
            文本 token 批量 Tensor，形状通常为 ``[B,L]``。
        """

        texts = [item.text for item in items]
        return await self.executors.run_cpu(self.runtime.tokenizer, texts)

    def _build_batch_task(self, items, tokens, trigger):
        """构造 BatchTask，并保持文本行与 Ticket 下标一致。

        Args:
            items: 当前批次的 TextPendingItem 列表。
            tokens: Tokenizer 返回的 token 批量 Tensor。
            trigger: 批次触发原因。

        Returns:
            文本 BatchTask。

        Raises:
            ValueError: token 批大小与 Ticket 数量不一致时抛出。
        """

        tickets = tuple(item.ticket for item in items)
        if tokens.shape[0] != len(tickets):
            raise ValueError("text token batch size mismatch")
        now = time.perf_counter()
        return BatchTask(
            batch_id=uuid.uuid4().hex,
            batch_type=BatchType.TEXT,
            tickets=tickets,
            input_tensor=tokens,
            trigger=trigger,
            created_at=now,
            gpu_enqueue_at=now,
        )

    async def _submit_batch(self, batch):
        """把仍然有效的文本批次提交到 GPU FIFO 队列。

        提交前再次检查 Ticket，避免同批中某个请求晚到超时或取消时拖垮其他请求。

        Args:
            batch: 待提交的文本 BatchTask。

        Returns:
            None。
        """

        batch = self._filter_submittable_batch(batch)
        if batch is None:
            return
        self._ensure_expected_state(batch.tickets, RequestState.QUEUED)
        for ticket in batch.tickets:
            ticket.transition(RequestState.QUEUED, RequestState.GPU_QUEUED)
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
        """用失败结果完成一组文本请求。

        Args:
            items: TextPendingItem 列表。
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
