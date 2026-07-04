import asyncio
from typing import Optional

import torch

from config import ENABLE_WARMUP, WARMUP_IMAGE_PATH, WARMUP_TEXT
from app.batching.executors import ModelTaskExecutors
from app.batching.gpu_batch_scheduler import GpuBatchScheduler
from app.batching.image_batch_processor import ImageBatchProcessor
from app.batching.image_preprocess_worker import ImagePreprocessWorker
from app.batching.properties import BatchingProperties
from app.batching.queue_registry import BatchQueueRegistry
from app.batching.text_batch_processor import TextBatchProcessor
from app.batching.ticket_factory import RequestTicketFactory
from app.batching.vectorization_service import AsyncVectorizationService
from app.inference.batch_inference_engine import BatchInferenceEngine
from app.inference.preprocessors import preprocess_image_sync


class ModelBatchingRuntime:
    """动态批处理服务的对象装配根。

    FastAPI 进程内只持有一个 runtime。这里负责装配 Queue、Executor、
    Processor 和 Scheduler，并且只把 ``AsyncVectorizationService`` 暴露给 API 层。
    """

    def __init__(self, openclip_runtime, properties: Optional[BatchingProperties] = None):
        """初始化动态批处理 runtime。

        Args:
            openclip_runtime: 已加载的 OpenCLIP 运行时对象。
            properties: 可选批处理配置；为空时使用默认配置。

        Returns:
            None。
        """

        self.openclip_runtime = openclip_runtime
        self.properties = properties or BatchingProperties()
        self.stop_event = asyncio.Event()
        self.executors = ModelTaskExecutors(
            cpu_max_workers=self.properties.executors.cpu_max_workers,
            gpu_max_workers=self.properties.executors.gpu_max_workers,
        )
        self.queues = BatchQueueRegistry(
            self.properties.queue.text_pending_maxsize,
            self.properties.queue.image_raw_maxsize,
            self.properties.queue.image_ready_maxsize,
            self.properties.queue.gpu_batch_maxsize,
        )
        self.ticket_factory = RequestTicketFactory(self.properties.request_timeout_ms)
        self.engine = BatchInferenceEngine(openclip_runtime)
        self.service = AsyncVectorizationService(self.queues, self.ticket_factory, openclip_runtime)
        self.text_processor = TextBatchProcessor(
            self.queues.text_pending_queue,
            self.queues.gpu_batch_queue,
            self.executors,
            openclip_runtime,
            self.properties.text,
            self.service,
            self.stop_event,
        )
        self.image_workers = [
            ImagePreprocessWorker(
                self.queues.image_raw_queue,
                self.queues.image_ready_queue,
                self.executors,
                openclip_runtime,
                self.service,
                self.stop_event,
                worker_id=index,
            )
            for index in range(self.properties.image.preprocess_worker_count)
        ]
        self.image_batch_processor = ImageBatchProcessor(
            self.queues.image_ready_queue,
            self.queues.gpu_batch_queue,
            self.executors,
            self.properties.image,
            self.service,
            self.stop_event,
        )
        self.gpu_scheduler = GpuBatchScheduler(
            self.queues.gpu_batch_queue,
            self.executors,
            self.engine,
            openclip_runtime.vector_dim,
            self.service,
            self.stop_event,
        )
        self.background_tasks: list[asyncio.Task] = []

    async def start(self) -> None:
        """启动线程池和后台协调协程。

        Args:
            None。

        Returns:
            None。
        """

        self.executors.start()
        self.service.start()
        self.background_tasks = [
            asyncio.create_task(self.text_processor.run(), name="text-batch-processor"),
            asyncio.create_task(self.image_batch_processor.run(), name="image-batch-processor"),
            asyncio.create_task(self.gpu_scheduler.run(), name="gpu-batch-scheduler"),
        ]
        for worker in self.image_workers:
            self.background_tasks.append(
                asyncio.create_task(worker.run(), name=f"image-preprocess-{worker.worker_id}")
            )

    async def warm_up(self) -> None:
        """通过直接推理预热模型，不让 warmup 请求进入业务队列。

        Args:
            None。

        Returns:
            None。
        """

        if not ENABLE_WARMUP:
            return
        image_bytes = WARMUP_IMAGE_PATH.read_bytes()
        await self._warm_up_image(image_bytes)
        await self._warm_up_text(WARMUP_TEXT)

    async def _warm_up_image(self, image_bytes: bytes) -> None:
        """执行一次图片预处理和 GPU 图片批量推理。

        Args:
            image_bytes: 用于 warmup 的图片二进制内容。

        Returns:
            None。
        """

        image_tensor = await self.executors.run_cpu(
            preprocess_image_sync,
            image_bytes,
            self.openclip_runtime.preprocess,
        )
        image_batch = await self.executors.run_cpu(torch.stack, [image_tensor])
        await self.executors.run_gpu(self.engine.infer_image_batch, image_batch)

    async def _warm_up_text(self, text: str) -> None:
        """执行一次文本 Tokenizer 和 GPU 文本批量推理。

        Args:
            text: 用于 warmup 的文本。

        Returns:
            None。
        """

        tokens = await self.executors.run_cpu(self.openclip_runtime.tokenizer, [text])
        await self.executors.run_gpu(self.engine.infer_text_batch, tokens)

    async def shutdown(self) -> None:
        """拒绝新请求、失败队列中 Ticket，然后停止后台任务。

        Args:
            None。

        Returns:
            None。
        """

        self.service.close()
        self._fail_queued_work("SERVICE_SHUTTING_DOWN", "vectorization service is shutting down")
        self.stop_event.set()
        for task in self.background_tasks:
            task.cancel()
        if self.background_tasks:
            await asyncio.gather(*self.background_tasks, return_exceptions=True)
        await self.executors.close()

    def _fail_queued_work(self, error_code: str, message: str) -> None:
        """清空所有队列，避免已接收请求在 shutdown 期间悬挂。

        Args:
            error_code: 写入失败结果的错误码。
            message: 写入失败结果的错误说明。

        Returns:
            None。
        """

        self._fail_queue_items(self.queues.text_pending_queue, error_code, message)
        self._fail_queue_items(self.queues.image_raw_queue, error_code, message)
        self._fail_queue_items(self.queues.image_ready_queue, error_code, message)
        self._fail_gpu_batches(error_code, message)

    def _fail_queue_items(self, queue, error_code: str, message: str) -> None:
        """在事件循环中使普通队列元素携带的 Ticket失败并删除。
        在清空队列的时候用于清理未完成任务

        Args:
            queue: 存放带 Ticket 元素的 asyncio.Queue。
            error_code: 写入失败结果的错误码。
            message: 写入失败结果的错误说明。

        Returns:
            None。
        """

        while True:
            try:
                item = queue.get_nowait()
            except asyncio.QueueEmpty:
                break
            item.ticket.try_fail(self.service.failure_result_for_ticket(item.ticket, error_code, message))
            queue.task_done()

    def _fail_gpu_batches(self, error_code: str, message: str) -> None:
        """失败已经组成 GPU 批次的 Ticket。

        Args:
            error_code: 写入失败结果的错误码。
            message: 写入失败结果的错误说明。

        Returns:
            None。
        """

        while True:
            try:
                batch = self.queues.gpu_batch_queue.get_nowait()
            except asyncio.QueueEmpty:
                break
            for ticket in batch.tickets:
                ticket.try_fail(self.service.failure_result_for_ticket(ticket, error_code, message))
            self.queues.gpu_batch_queue.task_done()
