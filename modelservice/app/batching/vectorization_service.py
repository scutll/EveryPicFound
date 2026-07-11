import asyncio
import time
from typing import Optional

from app.batching.enums import RequestState, VectorizeType
from app.batching.models import (
    HealthCheckResult,
    ImageRawItem,
    RequestTicket,
    TextPendingItem,
    VectorizeResult,
)
from app.log_utils import get_model_service_logger


logger = get_model_service_logger()


class AsyncVectorizationService:
    """面向 HTTP 层的异步向量化门面。

    每个 HTTP 请求创建一个 Ticket 和 Future，进入首个批处理队列后等待 Future。
    这里不直接执行 Tokenizer、图片预处理或模型推理。
    """

    def __init__(self, queue_registry, ticket_factory, runtime):
        """初始化异步向量化门面。

        Args:
            queue_registry: 批处理队列注册表。
            ticket_factory: RequestTicket 工厂。
            runtime: OpenCLIP 运行时对象。

        Returns:
            None。
        """

        self.queue_registry = queue_registry
        self.ticket_factory = ticket_factory
        self.runtime = runtime
        self.model_name = runtime.model_name if runtime is not None else ""
        self.accepting = True

    def start(self) -> None:
        """允许新请求进入队列。

        Args:
            None。

        Returns:
            None。
        """

        self.accepting = True

    def close(self) -> None:
        """关闭准入，shutdown 期间拒绝新请求。

        Args:
            None。

        Returns:
            None。
        """

        self.accepting = False

    async def vectorize_text(
        self,
        text: str,
        trace_id: Optional[str] = None,
        request_id: Optional[str] = None,
    ) -> VectorizeResult:
        """提交文本向量化请求并等待 Future 返回结果。

        Args:
            text: 待向量化文本。
            trace_id: 链路追踪 ID。
            request_id: HTTP 请求 ID。

        Returns:
            VectorizeResult，包含向量或失败信息。
        """

        start = time.perf_counter()
        if not self.accepting:
            return self.failure_result(None, VectorizeType.TEXT, "SERVICE_CLOSING", "vectorization service is closing", start)
        if not text or not text.strip():
            return self.failure_result(None, VectorizeType.TEXT, "TEXT_EMPTY", "text empty", start)

        ticket = self.ticket_factory.create(VectorizeType.TEXT, request_id, trace_id, None)
        ticket.transition(RequestState.CREATED, RequestState.QUEUED)
        item = TextPendingItem(ticket=ticket, text=text, enqueue_at=time.perf_counter())
        try:
            self.queue_registry.text_pending_queue.put_nowait(item)
        except asyncio.QueueFull:
            result = self.failure_result(ticket, VectorizeType.TEXT, "TEXT_QUEUE_FULL", "text queue full", start)
            ticket.try_fail(result)
            return result
        return await self._await_ticket(ticket, start)

    async def vectorize_image(
        self,
        image_bytes: bytes,
        image_id: Optional[int] = None,
        trace_id: Optional[str] = None,
        request_id: Optional[str] = None,
        original_file_name: Optional[str] = None,
        mime_type: Optional[str] = None,
    ) -> VectorizeResult:
        """提交图片向量化请求并等待 Future 返回结果。

        Args:
            image_bytes: 图片二进制内容。
            image_id: 图片业务 ID。
            trace_id: 链路追踪 ID。
            request_id: HTTP 请求 ID。
            original_file_name: 原始文件名。
            mime_type: 图片 MIME 类型。

        Returns:
            VectorizeResult，包含向量或失败信息。
        """

        start = time.perf_counter()
        if not self.accepting:
            return self.failure_result(None, VectorizeType.IMAGE, "SERVICE_CLOSING", "vectorization service is closing", start, image_id)
        if not image_bytes:
            return self.failure_result(None, VectorizeType.IMAGE, "IMAGE_EMPTY", "image file empty", start, image_id)

        ticket = self.ticket_factory.create(VectorizeType.IMAGE, request_id, trace_id, image_id)
        ticket.transition(RequestState.CREATED, RequestState.QUEUED)
        item = ImageRawItem(
            ticket=ticket,
            image_bytes=image_bytes,
            original_file_name=original_file_name,
            mime_type=mime_type,
            file_size=len(image_bytes),
            enqueue_at=time.perf_counter(),
        )
        try:
            self.queue_registry.image_raw_queue.put_nowait(item)
        except asyncio.QueueFull:
            result = self.failure_result(ticket, VectorizeType.IMAGE, "IMAGE_QUEUE_FULL", "image queue full", start, image_id)
            ticket.try_fail(result)
            return result
        return await self._await_ticket(ticket, start)

    async def _await_ticket(self, ticket: RequestTicket, start: float) -> VectorizeResult:
        """等待 Processor 或 Scheduler 向 Ticket Future 写入结果。

        Args:
            ticket: 当前 HTTP 请求对应的 Ticket。
            start: 请求开始时间，用于构造超时失败耗时。

        Returns:
            Future 中写入的 VectorizeResult，或超时失败结果。
        """

        timeout = max(0.001, ticket.deadline_at - time.perf_counter())
        try:
            return await asyncio.wait_for(asyncio.shield(ticket.future), timeout=timeout)
        except asyncio.TimeoutError:
            result = self.failure_result(ticket, ticket.vectorize_type, "REQUEST_TIMEOUT", "request timeout", start, ticket.image_id)
            ticket.try_fail(result)
            return result
        except asyncio.CancelledError:
            ticket.try_cancel()
            raise

    def success_result(self, ticket: RequestTicket, embedding: list[float], cost_ms: Optional[int] = None) -> VectorizeResult:
        """根据已完成 Ticket 构造稳定的 HTTP 成功响应。

        Args:
            ticket: 已完成的请求 Ticket。
            embedding: 当前请求对应的向量。
            cost_ms: 可选的显式耗时；为空时按 Ticket 到达时间计算。

        Returns:
            成功的 VectorizeResult。
        """

        cost = cost_ms
        if cost is None:
            cost = int((time.perf_counter() - ticket.arrival_at) * 1000)
        return VectorizeResult(
            success=True,
            vectorizeType=ticket.vectorize_type.value,
            imageId=ticket.image_id,
            embedding=embedding,
            dim=len(embedding),
            modelName=self.model_name,
            errorCode=None,
            message="success",
            costMs=cost,
        )

    def failure_result(
        self,
        ticket: Optional[RequestTicket],
        vectorize_type: VectorizeType,
        error_code: str,
        message: str,
        start: float,
        image_id: Optional[int] = None,
    ) -> VectorizeResult:
        """在尚未形成 Ticket 结果时构造失败响应。

        Args:
            ticket: 可选 Ticket；为空表示请求尚未成功入队。
            vectorize_type: 向量化类型。
            error_code: 错误码。
            message: 错误说明。
            start: 请求开始时间。
            image_id: 可选图片业务 ID。

        Returns:
            失败的 VectorizeResult。
        """

        return VectorizeResult(
            success=False,
            vectorizeType=vectorize_type.value,
            imageId=image_id if ticket is None else ticket.image_id,
            embedding=None,
            dim=0,
            modelName=self.model_name,
            errorCode=error_code,
            message=message,
            costMs=int((time.perf_counter() - start) * 1000),
        )

    def failure_result_for_ticket(self, ticket: RequestTicket, error_code: str, message: str) -> VectorizeResult:
        """根据 Ticket 身份和耗时信息构造失败响应。

        Args:
            ticket: 请求 Ticket。
            error_code: 错误码。
            message: 错误说明。

        Returns:
            失败的 VectorizeResult。
        """

        return VectorizeResult(
            success=False,
            vectorizeType=ticket.vectorize_type.value,
            imageId=ticket.image_id,
            embedding=None,
            dim=0,
            modelName=self.model_name,
            errorCode=error_code,
            message=message,
            costMs=int((time.perf_counter() - ticket.arrival_at) * 1000),
        )

    def check_health(self) -> HealthCheckResult:
        """检查模型运行时是否完整可用。

        Args:
            None。

        Returns:
            HealthCheckResult。
        """

        try:
            if self.runtime is None:
                return HealthCheckResult(False, "DOWN", False, "", 0, "", "MODEL_RUNTIME_NOT_INITIALIZED", "model runtime is not initialized")
            if self.runtime.model is None:
                return HealthCheckResult(False, "DOWN", False, self.runtime.model_name, self.runtime.vector_dim, self.runtime.device, "MODEL_NOT_LOADED", "model is not loaded")
            if self.runtime.preprocess is None:
                return HealthCheckResult(False, "DOWN", False, self.runtime.model_name, self.runtime.vector_dim, self.runtime.device, "IMAGE_PREPROCESS_NOT_LOADED", "image preprocess is not loaded")
            if self.runtime.tokenizer is None:
                return HealthCheckResult(False, "DOWN", False, self.runtime.model_name, self.runtime.vector_dim, self.runtime.device, "TEXT_TOKENIZER_NOT_LOADED", "text tokenizer is not loaded")
            return HealthCheckResult(True, "UP", True, self.runtime.model_name, self.runtime.vector_dim, self.runtime.device, None, "success")
        except Exception as exc:
            logger.exception(f"event=HEALTH_CHECK_FAILED message={str(exc)}")
            return HealthCheckResult(False, "DOWN", False, "", 0, "", "HEALTH_CHECK_ERROR", str(exc))
