import asyncio

from app.batching.enums import RequestState, VectorizeType
from app.batching.ticket_factory import RequestTicketFactory


def test_ticket_factory_creates_future_on_running_loop():
    async def exercise():
        factory = RequestTicketFactory(request_timeout_ms=15000)
        ticket = factory.create(VectorizeType.TEXT, "req-1", "trace-1", None)

        assert ticket.state == RequestState.CREATED
        assert ticket.future.get_loop() is asyncio.get_running_loop()
        assert ticket.deadline_at > ticket.arrival_at

    asyncio.run(exercise())
