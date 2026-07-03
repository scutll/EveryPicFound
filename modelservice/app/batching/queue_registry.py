import asyncio


class BatchQueueRegistry:
    """动态批处理链路中的全部 asyncio 队列。

    Queue 只在 FastAPI 事件循环中创建和访问。线程池函数必须把数据返回给协程，
    不能直接 put/get Queue。
    """

    def __init__(
        self,
        text_pending_maxsize: int,
        image_raw_maxsize: int,
        image_ready_maxsize: int,
        gpu_batch_maxsize: int,
    ):
        """创建四个有界队列。

        Args:
            text_pending_maxsize: 文本待 Tokenizer 队列最大长度。
            image_raw_maxsize: 图片原始请求队列最大长度。
            image_ready_maxsize: 图片预处理完成队列最大长度。
            gpu_batch_maxsize: GPU 批任务队列最大长度。

        Returns:
            None。
        """

        self.text_pending_queue = asyncio.Queue(maxsize=text_pending_maxsize)
        self.image_raw_queue = asyncio.Queue(maxsize=image_raw_maxsize)
        self.image_ready_queue = asyncio.Queue(maxsize=image_ready_maxsize)
        self.gpu_batch_queue = asyncio.Queue(maxsize=gpu_batch_maxsize)
