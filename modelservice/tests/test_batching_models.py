import asyncio

from app.batching.enums import RequestState, VectorizeType
from app.batching.models import RequestTicket, VectorizeResult


def test_request_ticket_completes_future_once():
    loop = asyncio.new_event_loop()
    try:
        future = loop.create_future()
        ticket = RequestTicket(
            task_id="task-1",
            request_id="req-1",
            trace_id="trace-1",
            vectorize_type=VectorizeType.TEXT,
            image_id=None,
            arrival_at=1.0,
            deadline_at=2.0,
            future=future,
            state=RequestState.QUEUED,
            batch_id=None,
            completed_at=None,
        )
        result = VectorizeResult(True, "TEXT", [1.0], 1, "clip", None, "success", 1)

        assert ticket.is_terminal() is False
        assert ticket.try_complete(result) is True
        assert ticket.try_complete(result) is False
        assert ticket.is_terminal() is True
        assert future.result() is result
    finally:
        loop.close()


def test_request_ticket_transition_rejects_wrong_state():
    loop = asyncio.new_event_loop()
    try:
        ticket = RequestTicket(
            task_id="task-1",
            request_id=None,
            trace_id=None,
            vectorize_type=VectorizeType.IMAGE,
            image_id=7,
            arrival_at=1.0,
            deadline_at=2.0,
            future=loop.create_future(),
            state=RequestState.CREATED,
            batch_id=None,
            completed_at=None,
        )

        try:
            ticket.transition(RequestState.QUEUED, RequestState.GPU_QUEUED)
        except ValueError as exc:
            assert "ticket state mismatch" in str(exc)
        else:
            raise AssertionError("expected transition to reject wrong source state")
    finally:
        loop.close()
