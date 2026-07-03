import asyncio
import time
from dataclasses import dataclass
from typing import Optional

import torch

from app.batching.enums import BatchTrigger, BatchType, RequestState, VectorizeType


@dataclass
class HealthCheckResult:
    """模型服务健康检查结果。

    该对象只承载健康检查数据，不参与批处理调度。
    """

    success: bool
    status: str
    modelLoaded: bool
    modelName: str
    vectorDim: int
    device: str
    errorCode: Optional[str]
    message: str


@dataclass
class VectorizeResult:
    """向量化统一返回结果。

    HTTP Controller 和 RequestTicket.future 都使用该对象承载成功或失败结果。
    """

    success: bool
    vectorizeType: str
    embedding: Optional[list[float]]
    dim: int
    modelName: str
    errorCode: Optional[str]
    message: str
    costMs: int
    imageId: Optional[int] = None


@dataclass
class RequestTicket:
    """单个 HTTP 向量化请求的身份与 Future 载体。

    Ticket 由 asyncio 事件循环拥有。线程池函数不能修改 Ticket，
    只能把计算结果或异常返回给原协程，由协程完成状态流转和 Future。
    """

    task_id: str
    request_id: Optional[str]
    trace_id: Optional[str]
    vectorize_type: VectorizeType
    image_id: Optional[int]
    arrival_at: float
    deadline_at: float
    future: asyncio.Future
    state: RequestState
    batch_id: Optional[str]
    completed_at: Optional[float]

    def is_terminal(self) -> bool:
        """判断 Ticket 是否已经进入终态。

        Args:
            None。

        Returns:
            已成功、失败或取消时返回 True，否则返回 False。
        """

        return self.state in {
            RequestState.SUCCEEDED,
            RequestState.FAILED,
            RequestState.CANCELLED,
        }

    def is_expired(self, now: float) -> bool:
        """判断请求是否已经超过总截止时间。

        Args:
            now: 当前单调时间，来自 ``time.perf_counter()``。

        Returns:
            当前时间达到或超过 ``deadline_at`` 时返回 True。
        """

        return now >= self.deadline_at

    def transition(self, expected: RequestState, target: RequestState) -> None:
        """仅在当前状态符合预期时执行状态流转。

        Args:
            expected: 期望的当前状态。
            target: 需要流转到的目标状态。

        Returns:
            None。

        Raises:
            ValueError: 当前状态与 expected 不一致时抛出。
        """

        if self.state != expected:
            raise ValueError(f"ticket state mismatch: expected={expected} actual={self.state}")
        self.state = target

    def try_complete(self, result: VectorizeResult) -> bool:
        """用成功结果完成 Future，且最多完成一次。

        Args:
            result: 写入 Future 的成功向量化结果。

        Returns:
            成功完成 Future 时返回 True；Future 已完成时返回 False。
        """

        if self.future.done():
            return False
        self.completed_at = time.perf_counter()
        self.state = RequestState.SUCCEEDED
        self.future.set_result(result)
        return True

    def try_fail(self, result: VectorizeResult) -> bool:
        """用失败结果完成 Future，且最多完成一次。

        Args:
            result: 写入 Future 的失败向量化结果。

        Returns:
            成功完成 Future 时返回 True；Future 已完成时返回 False。
        """

        if self.future.done():
            return False
        self.completed_at = time.perf_counter()
        self.state = RequestState.FAILED
        self.future.set_result(result)
        return True

    def try_cancel(self) -> bool:
        """在尚未产生结果时取消 Future。

        Args:
            None。

        Returns:
            成功取消 Future 时返回 True；Future 已完成时返回 False。
        """

        if self.future.done():
            return False
        self.completed_at = time.perf_counter()
        self.state = RequestState.CANCELLED
        self.future.cancel()
        return True


@dataclass
class TextPendingItem:
    """等待 CPU Tokenizer 处理的文本请求。"""

    ticket: RequestTicket
    text: str
    enqueue_at: float


@dataclass
class ImageRawItem:
    """等待 CPU 解码和 OpenCLIP transform 的图片请求。"""

    ticket: RequestTicket
    image_bytes: bytes
    original_file_name: Optional[str]
    mime_type: Optional[str]
    file_size: Optional[int]
    enqueue_at: float


@dataclass
class PreparedImageItem:
    """已完成 CPU 预处理、等待 stack 成图片批次的图片请求。"""

    ticket: RequestTicket
    image_tensor: torch.Tensor
    ready_at: float
    preprocess_cost_ms: int


@dataclass(frozen=True)
class BatchTask:
    """不可变 GPU 批任务。

    核心不变量是下标对应关系：``input_tensor[i]`` 属于 ``tickets[i]``。
    Scheduler 回填结果时也依赖相同顺序，把 ``embeddings[i]`` 写回对应 Ticket。
    """

    batch_id: str
    batch_type: BatchType
    tickets: tuple[RequestTicket, ...]
    input_tensor: torch.Tensor
    trigger: BatchTrigger
    created_at: float
    gpu_enqueue_at: float


@dataclass(frozen=True)
class BatchInferenceResult:
    """单线程 GPU executor 返回的批量推理结果。"""

    embeddings: torch.Tensor
    batch_size: int
    vector_dim: int
    inference_cost_ms: int
