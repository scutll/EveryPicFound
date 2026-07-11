from enum import Enum


class VectorizeType(str, Enum):
    TEXT = "TEXT"
    IMAGE = "IMAGE"


class BatchType(str, Enum):
    TEXT = "TEXT"
    IMAGE = "IMAGE"


class BatchTrigger(str, Enum):
    SIZE = "SIZE"
    TIME = "TIME"
    DRAIN = "DRAIN"


class RequestState(str, Enum):
    CREATED = "CREATED"
    QUEUED = "QUEUED"
    PREPROCESSING = "PREPROCESSING"
    READY = "READY"
    GPU_QUEUED = "GPU_QUEUED"
    INFERENCING = "INFERENCING"
    SUCCEEDED = "SUCCEEDED"
    FAILED = "FAILED"
    CANCELLED = "CANCELLED"
