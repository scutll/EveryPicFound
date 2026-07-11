import asyncio

from app.batching.enums import VectorizeType
from app.batching.queue_registry import BatchQueueRegistry
from app.batching.ticket_factory import RequestTicketFactory
from app.batching.vectorization_service import AsyncVectorizationService


class FakeRuntime:
    model_name = "clip-test"
    model = object()
    preprocess = object()
    tokenizer = object()
    vector_dim = 2
    device = "cpu"


def test_vectorize_text_enqueues_item_and_waits_for_future():
    async def exercise():
        registry = BatchQueueRegistry(4, 4, 4, 4)
        factory = RequestTicketFactory(request_timeout_ms=1000)
        service = AsyncVectorizationService(registry, factory, FakeRuntime())

        task = asyncio.create_task(service.vectorize_text("hello", "trace-1", "req-1"))
        item = await registry.text_pending_queue.get()

        assert item.text == "hello"
        assert item.ticket.vectorize_type == VectorizeType.TEXT

        item.ticket.try_complete(service.success_result(item.ticket, [0.1, 0.2]))
        result = await task

        assert result.success is True
        assert result.embedding == [0.1, 0.2]

    asyncio.run(exercise())


def test_vectorize_text_returns_queue_full_when_pending_queue_is_full():
    async def exercise():
        registry = BatchQueueRegistry(1, 4, 4, 4)
        factory = RequestTicketFactory(request_timeout_ms=1000)
        service = AsyncVectorizationService(registry, factory, FakeRuntime())
        await registry.text_pending_queue.put(object())

        result = await service.vectorize_text("hello", "trace-1", "req-1")

        assert result.success is False
        assert result.errorCode == "TEXT_QUEUE_FULL"

    asyncio.run(exercise())


def test_vectorize_text_rejects_requests_after_service_close():
    async def exercise():
        registry = BatchQueueRegistry(1, 4, 4, 4)
        factory = RequestTicketFactory(request_timeout_ms=1000)
        service = AsyncVectorizationService(registry, factory, FakeRuntime())
        service.close()

        result = await service.vectorize_text("hello", "trace-1", "req-1")

        assert result.success is False
        assert result.errorCode == "SERVICE_CLOSING"
        assert registry.text_pending_queue.empty()

    asyncio.run(exercise())
