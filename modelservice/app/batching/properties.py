from dataclasses import dataclass

from config import (
    CPU_EXECUTOR_MAX_WORKERS,
    GPU_BATCH_QUEUE_MAXSIZE,
    GPU_EXECUTOR_MAX_WORKERS,
    IMAGE_BATCH_MAX_SIZE,
    IMAGE_BATCH_MAX_WAIT_MS,
    IMAGE_PREPROCESS_WORKER_COUNT,
    IMAGE_RAW_QUEUE_MAXSIZE,
    IMAGE_READY_QUEUE_MAXSIZE,
    REQUEST_TIMEOUT_MS,
    TEXT_BATCH_MAX_SIZE,
    TEXT_BATCH_MAX_WAIT_MS,
    TEXT_PENDING_QUEUE_MAXSIZE,
)


@dataclass(frozen=True)
class TextBatchProperties:
    max_batch_size: int = TEXT_BATCH_MAX_SIZE
    max_wait_ms: int = TEXT_BATCH_MAX_WAIT_MS


@dataclass(frozen=True)
class ImageBatchProperties:
    max_batch_size: int = IMAGE_BATCH_MAX_SIZE
    max_wait_ms: int = IMAGE_BATCH_MAX_WAIT_MS
    preprocess_worker_count: int = IMAGE_PREPROCESS_WORKER_COUNT


@dataclass(frozen=True)
class QueueProperties:
    text_pending_maxsize: int = TEXT_PENDING_QUEUE_MAXSIZE
    image_raw_maxsize: int = IMAGE_RAW_QUEUE_MAXSIZE
    image_ready_maxsize: int = IMAGE_READY_QUEUE_MAXSIZE
    gpu_batch_maxsize: int = GPU_BATCH_QUEUE_MAXSIZE


@dataclass(frozen=True)
class ExecutorProperties:
    cpu_max_workers: int = CPU_EXECUTOR_MAX_WORKERS
    gpu_max_workers: int = GPU_EXECUTOR_MAX_WORKERS


@dataclass(frozen=True)
class BatchingProperties:
    text: TextBatchProperties = TextBatchProperties()
    image: ImageBatchProperties = ImageBatchProperties()
    queue: QueueProperties = QueueProperties()
    executors: ExecutorProperties = ExecutorProperties()
    request_timeout_ms: int = REQUEST_TIMEOUT_MS
