import asyncio
import time
import uuid
from typing import Optional

from app.batching.enums import RequestState, VectorizeType
from app.batching.models import RequestTicket


class RequestTicketFactory:
    def __init__(self, request_timeout_ms: int):
        self.request_timeout_ms = request_timeout_ms

    def create(
        self,
        vectorize_type: VectorizeType,
        request_id: Optional[str],
        trace_id: Optional[str],
        image_id: Optional[int],
    ) -> RequestTicket:
        loop = asyncio.get_running_loop()
        arrival_at = time.perf_counter()
        deadline_at = arrival_at + (self.request_timeout_ms / 1000.0)
        return RequestTicket(
            task_id=uuid.uuid4().hex,
            request_id=request_id,
            trace_id=trace_id,
            vectorize_type=vectorize_type,
            image_id=image_id,
            arrival_at=arrival_at,
            deadline_at=deadline_at,
            future=loop.create_future(),
            state=RequestState.CREATED,
            batch_id=None,
            completed_at=None,
        )
