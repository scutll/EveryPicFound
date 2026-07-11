import asyncio
import time

from PIL import UnidentifiedImageError

from app.batching.enums import RequestState
from app.batching.models import PreparedImageItem
from app.inference.preprocessors import preprocess_image_sync
from app.log_utils import get_model_service_logger


logger = get_model_service_logger()


class ImagePreprocessWorker:
    """图片 CPU 预处理 Worker。

    多个 Worker 协程可以同时消费 ``ImageRawQueue``。耗时的图片解码和 transform
    在线程池执行；Queue、Ticket 和 Future 只由协程在事件循环中操作。
    """

    def __init__(self, raw_queue, ready_queue, executors, runtime, result_builder, stop_event, worker_id):
        """初始化图片预处理 Worker。

        Args:
            raw_queue: 图片原始请求队列。
            ready_queue: 图片预处理完成队列。
            executors: CPU/GPU 线程池容器。
            runtime: OpenCLIP 运行时对象。
            result_builder: 用于构造失败结果的服务门面。
            stop_event: 服务关闭事件。
            worker_id: Worker 编号，用于日志定位。

        Returns:
            None。
        """

        self.raw_queue = raw_queue
        self.ready_queue = ready_queue
        self.executors = executors
        self.runtime = runtime
        self.result_builder = result_builder
        self.stop_event = stop_event
        self.worker_id = worker_id

    async def run(self) -> None:
        """消费原始图片，并发布已预处理的图片 Tensor。

        Args:
            None。

        Returns:
            None。
        """

        while not self.stop_event.is_set():
            raw_item = None
            try:
                raw_item = await self.raw_queue.get()
                if raw_item.ticket.is_terminal():
                    continue
                if raw_item.ticket.is_expired(time.perf_counter()):
                    raw_item.ticket.try_fail(self.result_builder.failure_result_for_ticket(raw_item.ticket, "REQUEST_TIMEOUT", "request timeout"))
                    continue
                raw_item.ticket.transition(RequestState.QUEUED, RequestState.PREPROCESSING)
                start = time.perf_counter()
                tensor = await self._preprocess(raw_item)
                ready_item = self._build_ready_item(
                    raw_item,
                    tensor,
                    int((time.perf_counter() - start) * 1000),
                )
                raw_item.ticket.transition(RequestState.PREPROCESSING, RequestState.READY)
                await self.ready_queue.put(ready_item)
            except asyncio.CancelledError:
                if raw_item is not None:
                    raw_item.ticket.try_fail(
                        self.result_builder.failure_result_for_ticket(
                            raw_item.ticket,
                            "SERVICE_SHUTTING_DOWN",
                            "vectorization service is shutting down",
                        )
                    )
                raise
            except UnidentifiedImageError:
                if raw_item is not None:
                    raw_item.ticket.try_fail(self.result_builder.failure_result_for_ticket(raw_item.ticket, "IMAGE_DECODE_ERROR", "image decode failed"))
            except Exception as exc:
                logger.exception(f"event=IMAGE_PREPROCESS_FAILED workerId={self.worker_id} message={str(exc)}")
                if raw_item is not None:
                    raw_item.ticket.try_fail(self.result_builder.failure_result_for_ticket(raw_item.ticket, "IMAGE_PREPROCESS_ERROR", str(exc)))
            finally:
                if raw_item is not None:
                    self.raw_queue.task_done()

    async def _preprocess(self, raw_item):
        """在线程池中解码并转换单张图片。

        Args:
            raw_item: ImageRawItem，包含图片字节和 Ticket。

        Returns:
            单张图片 Tensor，形状通常为 ``[C,H,W]``。
        """

        return await self.executors.run_cpu(preprocess_image_sync, raw_item.image_bytes, self.runtime.preprocess)

    @staticmethod
    def _build_ready_item(raw_item, tensor, preprocess_cost_ms):
        """把原始 Ticket 和预处理后的 Tensor 继续绑定在一起。

        Args:
            raw_item: 原始图片请求。
            tensor: 预处理后的图片 Tensor。
            preprocess_cost_ms: 预处理耗时，单位毫秒。

        Returns:
            PreparedImageItem。
        """

        return PreparedImageItem(
            ticket=raw_item.ticket,
            image_tensor=tensor,
            ready_at=time.perf_counter(),
            preprocess_cost_ms=preprocess_cost_ms,
        )
