import asyncio

import torch

from app.batching.enums import VectorizeType
from app.batching.models import TextPendingItem
from app.batching.properties import (
    BatchingProperties,
    ExecutorProperties,
    ImageBatchProperties,
    QueueProperties,
    TextBatchProperties,
)
from app.batching.runtime import ModelBatchingRuntime


class FakeModel:
    def __init__(self):
        self.text_batch_sizes = []

    def encode_text(self, tokens):
        self.text_batch_sizes.append(tokens.shape[0])
        return tokens.float()

    def encode_image(self, images):
        return torch.ones((images.shape[0], 2))


class FakeOpenClipRuntime:
    def __init__(self):
        self.model = FakeModel()
        self.preprocess = lambda image: torch.ones((3, 4, 4))
        self.tokenizer = lambda texts: torch.tensor([[1.0, 0.0] for _ in texts])
        self.device = "cpu"
        self.model_name = "clip-test"
        self.vector_dim = 2


def test_two_text_requests_share_one_gpu_batch():
    async def exercise():
        runtime = FakeOpenClipRuntime()
        batching_runtime = ModelBatchingRuntime(
            runtime,
            BatchingProperties(
                text=TextBatchProperties(max_batch_size=2, max_wait_ms=100),
                image=ImageBatchProperties(max_batch_size=2, max_wait_ms=100, preprocess_worker_count=1),
                queue=QueueProperties(8, 8, 8, 8),
                executors=ExecutorProperties(cpu_max_workers=2, gpu_max_workers=1),
                request_timeout_ms=5000,
            ),
        )
        await batching_runtime.start()
        try:
            result_a, result_b = await asyncio.gather(
                batching_runtime.service.vectorize_text("a", "trace", "req-a"),
                batching_runtime.service.vectorize_text("b", "trace", "req-b"),
            )
        finally:
            await batching_runtime.shutdown()

        assert result_a.success is True
        assert result_b.success is True
        assert runtime.model.text_batch_sizes == [2]

    asyncio.run(exercise())


def test_shutdown_fails_queued_text_request_future():
    async def exercise():
        runtime = FakeOpenClipRuntime()
        batching_runtime = ModelBatchingRuntime(
            runtime,
            BatchingProperties(
                text=TextBatchProperties(max_batch_size=2, max_wait_ms=100),
                image=ImageBatchProperties(max_batch_size=2, max_wait_ms=100, preprocess_worker_count=1),
                queue=QueueProperties(8, 8, 8, 8),
                executors=ExecutorProperties(cpu_max_workers=2, gpu_max_workers=1),
                request_timeout_ms=5000,
            ),
        )
        ticket = batching_runtime.ticket_factory.create(
            vectorize_type=VectorizeType.TEXT,
            request_id="req",
            trace_id="trace",
            image_id=None,
        )
        await batching_runtime.queues.text_pending_queue.put(
            TextPendingItem(ticket=ticket, text="queued", enqueue_at=0.0)
        )

        await batching_runtime.shutdown()

        assert ticket.future.done()
        assert ticket.future.result().success is False
        assert ticket.future.result().errorCode == "SERVICE_SHUTTING_DOWN"

    asyncio.run(exercise())


def test_warmup_text_uses_direct_inference_without_enqueuing_request():
    async def exercise():
        runtime = FakeOpenClipRuntime()
        batching_runtime = ModelBatchingRuntime(
            runtime,
            BatchingProperties(
                text=TextBatchProperties(max_batch_size=2, max_wait_ms=100),
                image=ImageBatchProperties(max_batch_size=2, max_wait_ms=100, preprocess_worker_count=1),
                queue=QueueProperties(8, 8, 8, 8),
                executors=ExecutorProperties(cpu_max_workers=2, gpu_max_workers=1),
                request_timeout_ms=5000,
            ),
        )
        await batching_runtime.start()
        try:
            await batching_runtime._warm_up_text("warmup")
        finally:
            await batching_runtime.shutdown()

        assert runtime.model.text_batch_sizes == [1]
        assert batching_runtime.queues.text_pending_queue.empty()
        assert batching_runtime.queues.gpu_batch_queue.empty()

    asyncio.run(exercise())
