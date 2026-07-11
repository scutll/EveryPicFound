import asyncio
import time

import torch

from app.batching.enums import BatchType, RequestState, VectorizeType
from app.batching.image_batch_processor import ImageBatchProcessor
from app.batching.image_preprocess_worker import ImagePreprocessWorker
from app.batching.models import ImageRawItem, PreparedImageItem, RequestTicket, VectorizeResult


class FakeExecutors:
    async def run_cpu(self, func, *args):
        return func(*args)


class FakeRuntime:
    preprocess = staticmethod(lambda image: torch.ones((3, 4, 4)))


class FakeResultBuilder:
    def failure_result_for_ticket(self, ticket, error_code, message):
        return VectorizeResult(False, ticket.vectorize_type.value, None, 0, "clip", error_code, message, 1, ticket.image_id)


def make_image_ticket(loop):
    return RequestTicket(
        task_id="task",
        request_id=None,
        trace_id=None,
        vectorize_type=VectorizeType.IMAGE,
        image_id=1,
        arrival_at=time.perf_counter(),
        deadline_at=time.perf_counter() + 10,
        future=loop.create_future(),
        state=RequestState.READY,
        batch_id=None,
        completed_at=None,
    )


def test_image_batch_processor_stacks_ready_items():
    async def exercise():
        loop = asyncio.get_running_loop()
        ready_queue = asyncio.Queue()
        gpu_queue = asyncio.Queue()
        ticket = make_image_ticket(loop)
        item = PreparedImageItem(ticket, torch.ones((3, 4, 4)), time.perf_counter(), 2)
        processor = ImageBatchProcessor(
            ready_queue,
            gpu_queue,
            FakeExecutors(),
            type("P", (), {"max_batch_size": 2, "max_wait_ms": 1})(),
            FakeResultBuilder(),
            asyncio.Event(),
        )

        batch_tensor = await processor._stack_batch([item])
        batch = processor._build_batch_task([item], batch_tensor, "TIME")

        assert batch.batch_type == BatchType.IMAGE
        assert batch.input_tensor.shape == (1, 3, 4, 4)

    asyncio.run(exercise())


def test_image_preprocess_worker_preserves_ticket_on_ready_item():
    async def exercise():
        loop = asyncio.get_running_loop()
        worker = ImagePreprocessWorker(
            asyncio.Queue(),
            asyncio.Queue(),
            FakeExecutors(),
            FakeRuntime(),
            FakeResultBuilder(),
            asyncio.Event(),
            worker_id=0,
        )
        ticket = make_image_ticket(loop)
        raw = ImageRawItem(ticket, b"not-used", "a.png", "image/png", 8, time.perf_counter())
        ready = worker._build_ready_item(raw, torch.ones((3, 4, 4)), 3)

        assert ready.ticket is ticket

    asyncio.run(exercise())
