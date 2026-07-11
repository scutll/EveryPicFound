import asyncio

from app.batching.queue_registry import BatchQueueRegistry


def test_queue_registry_applies_configured_maxsizes():
    async def exercise():
        registry = BatchQueueRegistry(1, 2, 3, 4)

        assert registry.text_pending_queue.maxsize == 1
        assert registry.image_raw_queue.maxsize == 2
        assert registry.image_ready_queue.maxsize == 3
        assert registry.gpu_batch_queue.maxsize == 4

    asyncio.run(exercise())
