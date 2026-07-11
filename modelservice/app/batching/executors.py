import asyncio
from concurrent.futures import ThreadPoolExecutor
from typing import Any, Callable, Optional


class ModelTaskExecutors:
    """阻塞型 CPU/GPU 工作的线程池边界。

    Executor 不感知 Queue、Ticket 或 Future。Processor 只提交纯计算函数，
    计算结果或异常回到事件循环后再更新业务状态。
    """

    def __init__(self, cpu_max_workers: int, gpu_max_workers: int):
        """初始化 CPU 和 GPU executor 配置。

        Args:
            cpu_max_workers: CPU 线程池最大工作线程数。
            gpu_max_workers: GPU 线程池最大工作线程数，当前设计固定为 1。

        Returns:
            None。
        """

        self.cpu_max_workers = cpu_max_workers
        self.gpu_max_workers = gpu_max_workers
        self.cpu_executor: Optional[ThreadPoolExecutor] = None
        self.gpu_executor: Optional[ThreadPoolExecutor] = None
        self.started = False
        self.closed = False

    def start(self) -> None:
        """创建 CPU 线程池和单线程 GPU 线程池。

        Args:
            None。

        Returns:
            None。
        """

        if self.started:
            return
        self.cpu_executor = ThreadPoolExecutor(
            max_workers=self.cpu_max_workers,
            thread_name_prefix="model-cpu",
        )
        self.gpu_executor = ThreadPoolExecutor(
            max_workers=self.gpu_max_workers,
            thread_name_prefix="model-gpu",
        )
        self.started = True
        self.closed = False

    async def run_cpu(self, func: Callable[..., Any], *args: Any) -> Any:
        """在线程池中运行阻塞型 CPU 准备工作。

        Args:
            func: 需要在线程池中执行的同步函数。
            *args: 传递给同步函数的位置参数。

        Returns:
            同步函数的返回值。

        Raises:
            RuntimeError: CPU executor 尚未启动时抛出。
        """

        if self.cpu_executor is None:
            raise RuntimeError("cpu executor not started")
        loop = asyncio.get_running_loop()
        return await loop.run_in_executor(self.cpu_executor, func, *args)

    async def run_gpu(self, func: Callable[..., Any], *args: Any) -> Any:
        """通过串行 GPU executor 运行模型推理。

        Args:
            func: 需要在 GPU executor 中执行的同步推理函数。
            *args: 传递给推理函数的位置参数。

        Returns:
            同步推理函数的返回值。

        Raises:
            RuntimeError: GPU executor 尚未启动时抛出。
        """

        if self.gpu_executor is None:
            raise RuntimeError("gpu executor not started")
        loop = asyncio.get_running_loop()
        return await loop.run_in_executor(self.gpu_executor, func, *args)

    async def close(self) -> None:
        """关闭 CPU 和 GPU 线程池。

        Args:
            None。

        Returns:
            None。
        """

        if self.cpu_executor is not None:
            self.cpu_executor.shutdown(wait=True, cancel_futures=False)
            self.cpu_executor = None
        if self.gpu_executor is not None:
            self.gpu_executor.shutdown(wait=True, cancel_futures=False)
            self.gpu_executor = None
        self.closed = True
        self.started = False
