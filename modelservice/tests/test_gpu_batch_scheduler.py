import asyncio
import time

import torch

from app.batching.enums import BatchTrigger, BatchType, RequestState, VectorizeType
from app.batching.gpu_batch_scheduler import GpuBatchScheduler
from app.batching.models import BatchInferenceResult, BatchTask, RequestTicket, VectorizeResult


class FakeResultBuilder:
    def success_result(self, ticket, embedding, cost_ms=None):
        return VectorizeResult(True, ticket.vectorize_type.value, embedding, len(embedding), "clip", None, "success", 1, ticket.image_id)

    def failure_result_for_ticket(self, ticket, error_code, message):
        return VectorizeResult(False, ticket.vectorize_type.value, None, 0, "clip", error_code, message, 1, ticket.image_id)


def test_complete_batch_maps_embedding_by_ticket_index():
    async def exercise():
        loop = asyncio.get_running_loop()
        tickets = []
        for index in range(2):
            tickets.append(
                RequestTicket(
                    task_id=f"task-{index}",
                    request_id=None,
                    trace_id=None,
                    vectorize_type=VectorizeType.TEXT,
                    image_id=None,
                    arrival_at=time.perf_counter(),
                    deadline_at=time.perf_counter() + 10,
                    future=loop.create_future(),
                    state=RequestState.INFERENCING,
                    batch_id="batch",
                    completed_at=None,
                )
            )
        batch = BatchTask(
            batch_id="batch",
            batch_type=BatchType.TEXT,
            tickets=tuple(tickets),
            input_tensor=torch.ones((2, 2)),
            trigger=BatchTrigger.SIZE,
            created_at=time.perf_counter(),
            gpu_enqueue_at=time.perf_counter(),
        )
        result = BatchInferenceResult(torch.tensor([[1.0, 2.0], [3.0, 4.0]]), 2, 2, 1)
        scheduler = GpuBatchScheduler(asyncio.Queue(), None, None, 2, FakeResultBuilder(), asyncio.Event())

        scheduler._validate_batch_result(batch, result)
        scheduler._complete_batch(batch, result)

        assert tickets[0].future.result().embedding == [1.0, 2.0]
        assert tickets[1].future.result().embedding == [3.0, 4.0]

    asyncio.run(exercise())
