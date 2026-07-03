import asyncio
import time

import torch

from app.batching.enums import BatchTrigger, BatchType, RequestState, VectorizeType
from app.batching.models import BatchTask, RequestTicket, TextPendingItem, VectorizeResult
from app.batching.text_batch_processor import TextBatchProcessor


class FakeExecutors:
    async def run_cpu(self, func, *args):
        return func(*args)


class FakeRuntime:
    def tokenizer(self, texts):
        return torch.ones((len(texts), 2), dtype=torch.long)


class FakeResultBuilder:
    def failure_result_for_ticket(self, ticket, error_code, message):
        return VectorizeResult(False, ticket.vectorize_type.value, None, 0, "clip", error_code, message, 1, ticket.image_id)


def make_ticket(loop):
    return RequestTicket(
        task_id="task",
        request_id=None,
        trace_id=None,
        vectorize_type=VectorizeType.TEXT,
        image_id=None,
        arrival_at=time.perf_counter(),
        deadline_at=time.perf_counter() + 10,
        future=loop.create_future(),
        state=RequestState.QUEUED,
        batch_id=None,
        completed_at=None,
    )


def test_text_processor_collects_full_batch_and_builds_task():
    async def exercise():
        loop = asyncio.get_running_loop()
        pending_queue = asyncio.Queue()
        gpu_queue = asyncio.Queue()
        for text in ("a", "b"):
            await pending_queue.put(TextPendingItem(make_ticket(loop), text, time.perf_counter()))

        processor = TextBatchProcessor(
            pending_queue,
            gpu_queue,
            FakeExecutors(),
            FakeRuntime(),
            type("P", (), {"max_batch_size": 2, "max_wait_ms": 50})(),
            FakeResultBuilder(),
            asyncio.Event(),
        )

        items, trigger = await processor._collect_batch()
        tokens = await processor._tokenize_batch(items)
        batch = processor._build_batch_task(items, tokens, trigger)

        assert trigger == BatchTrigger.SIZE
        assert batch.batch_type == BatchType.TEXT
        assert batch.input_tensor.shape[0] == 2

    asyncio.run(exercise())


def test_submit_batch_skips_terminal_ticket_without_stranding_active_ticket():
    async def exercise():
        loop = asyncio.get_running_loop()
        gpu_queue = asyncio.Queue()
        processor = TextBatchProcessor(
            asyncio.Queue(),
            gpu_queue,
            FakeExecutors(),
            FakeRuntime(),
            type("P", (), {"max_batch_size": 2, "max_wait_ms": 50})(),
            FakeResultBuilder(),
            asyncio.Event(),
        )
        active_ticket = make_ticket(loop)
        terminal_ticket = make_ticket(loop)
        terminal_ticket.try_fail(VectorizeResult(False, "TEXT", None, 0, "clip", "FAILED", "failed", 1))
        batch = BatchTask(
            batch_id="batch",
            batch_type=BatchType.TEXT,
            tickets=(active_ticket, terminal_ticket),
            input_tensor=torch.ones((2, 2)),
            trigger=BatchTrigger.SIZE,
            created_at=time.perf_counter(),
            gpu_enqueue_at=time.perf_counter(),
        )

        await processor._submit_batch(batch)
        queued_batch = await gpu_queue.get()

        assert queued_batch.tickets == (active_ticket,)
        assert queued_batch.input_tensor.shape[0] == 1
        assert active_ticket.state == RequestState.GPU_QUEUED

    asyncio.run(exercise())
