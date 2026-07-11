import asyncio

from app.batching.executors import ModelTaskExecutors


def test_run_cpu_executes_function_and_returns_result():
    async def exercise():
        executors = ModelTaskExecutors(cpu_max_workers=1, gpu_max_workers=1)
        executors.start()
        try:
            result = await executors.run_cpu(lambda value: value + 1, 3)
            assert result == 4
        finally:
            await executors.close()

    asyncio.run(exercise())
