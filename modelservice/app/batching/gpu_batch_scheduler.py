import asyncio
import time

from app.batching.enums import BatchType, RequestState
from app.log_utils import get_model_service_logger


logger = get_model_service_logger()


class GpuBatchScheduler:
    """文本和图片 GPU 批次的唯一消费者。

    Scheduler 在批次级别保持 FIFO 顺序，所有 OpenCLIP 前向计算都通过
    单线程 GPU executor 串行执行。
    """

    def __init__(self, gpu_batch_queue, executors, engine, runtime_vector_dim, result_builder, stop_event):
        """初始化 GPU 批调度器。

        Args:
            gpu_batch_queue: 统一 GPU 批任务队列。
            executors: CPU/GPU 线程池容器。
            engine: 批量推理引擎。
            runtime_vector_dim: 模型配置的向量维度。
            result_builder: 用于构造成功/失败结果的服务门面。
            stop_event: 服务关闭事件。

        Returns:
            None。
        """

        self.gpu_batch_queue = gpu_batch_queue
        self.executors = executors
        self.engine = engine
        self.runtime_vector_dim = runtime_vector_dim
        self.result_builder = result_builder
        self.stop_event = stop_event

    async def run(self) -> None:
        """执行 GPU 批次，并按下标完成每个 Ticket Future。

        Args:
            None。

        Returns:
            None。
        """

        while not self.stop_event.is_set():
            batch = None
            try:
                batch = await self.gpu_batch_queue.get()
                active_batch = self._mark_inferencing(batch)
                if active_batch is None:
                    continue
                result = await self._execute_batch(active_batch)
                self._validate_batch_result(active_batch, result)
                self._complete_batch(active_batch, result)
            except asyncio.CancelledError:
                if batch is not None:
                    self._fail_batch(batch, "SERVICE_SHUTTING_DOWN", "vectorization service is shutting down")
                raise
            except Exception as exc:
                logger.exception(f"event=GPU_BATCH_FAILED message={str(exc)}")
                if batch is not None:
                    self._fail_batch(batch, "MODEL_INFERENCE_ERROR", str(exc))
            finally:
                if batch is not None:
                    self.gpu_batch_queue.task_done()

    def _mark_inferencing(self, batch):
        """GPU 执行前把仍然有效的 Ticket 流转到 INFERENCING。

        Args:
            batch: 待执行的 BatchTask。

        Returns:
            仍有有效 Ticket 时返回原 BatchTask；全部失效时返回 None。
        """

        now = time.perf_counter()
        has_active_ticket = False
        for ticket in batch.tickets:
            if ticket.is_terminal():
                continue
            if ticket.is_expired(now):
                ticket.try_fail(self.result_builder.failure_result_for_ticket(ticket, "REQUEST_TIMEOUT", "request timeout"))
                continue
            ticket.transition(RequestState.GPU_QUEUED, RequestState.INFERENCING)
            has_active_ticket = True
        if not has_active_ticket:
            return None
        return batch

    async def _execute_batch(self, batch):
        """根据批次类型分派到对应的批量推理函数。

        Args:
            batch: 待执行的 BatchTask。

        Returns:
            BatchInferenceResult。

        Raises:
            ValueError: 遇到不支持的批次类型时抛出。
        """

        if batch.batch_type == BatchType.TEXT:
            return await self.executors.run_gpu(self.engine.infer_text_batch, batch.input_tensor)
        if batch.batch_type == BatchType.IMAGE:
            return await self.executors.run_gpu(self.engine.infer_image_batch, batch.input_tensor)
        raise ValueError(f"unsupported batch type: {batch.batch_type}")

    def _validate_batch_result(self, batch, result):
        """在逐个回填前校验 GPU 批量结果是否可靠。

        Args:
            batch: 原始 BatchTask。
            result: GPU 批量推理结果。

        Returns:
            None。

        Raises:
            ValueError: 批大小或向量维度不匹配时抛出。
        """

        if result.batch_size != len(batch.tickets):
            raise ValueError("batch result size mismatch")
        if result.vector_dim != self.runtime_vector_dim:
            raise ValueError("batch vector dim mismatch")

    def _complete_batch(self, batch, result):
        """把 ``embeddings[i]`` 映射到 ``tickets[i]`` 并完成 Future。

        Args:
            batch: 已完成推理的 BatchTask。
            result: GPU 批量推理结果。

        Returns:
            None。
        """

        for index, ticket in enumerate(batch.tickets):
            if ticket.is_terminal():
                continue
            embedding = result.embeddings[index].tolist()
            ticket.try_complete(self.result_builder.success_result(ticket, embedding))

    def _fail_batch(self, batch, error_code, message):
        """失败时完成批次内所有尚未终态的 Ticket。

        Args:
            batch: 失败的 BatchTask。
            error_code: 错误码。
            message: 错误说明。

        Returns:
            None。
        """

        for ticket in batch.tickets:
            ticket.try_fail(self.result_builder.failure_result_for_ticket(ticket, error_code, message))
