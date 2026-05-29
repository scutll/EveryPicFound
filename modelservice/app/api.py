from fastapi import APIRouter, File, Form, Request, UploadFile

from app.inference_lock import inference_lock
from app.schemas import ImageVectorizeRequest, TextVectorizeRequest
from app.schemas import HealthResponse, VectorizeResponse
from app.vectorization_service import VectorizeResult
from app.log_utils import get_model_service_logger


router = APIRouter()
logger = get_model_service_logger()


def get_vectorization_service(request: Request):
    #从全局状态中获取vectorization service对象
    service = getattr(request.app.state, "vectorization_service", None)
    
    if service is None:
        raise RuntimeError("vectorization service uninitialized")
    
    return service



def build_vectorize_response(result:VectorizeResult) -> VectorizeResponse:
    return VectorizeResponse(
        success=result.success,
        vectorizeType=result.vectorizeType,
        imageId=result.imageId,
        embedding=result.embedding,
        dim=result.dim,
        modelName=result.modelName,
        errorCode=result.errorCode,
        message=result.message,
        costMs=result.costMs,
    )
    

@router.get("/health", response_model=HealthResponse)
async def health(request: Request):
    
    try:
        service = get_vectorization_service(request)
        result = service.check_health()
        
        
        logger.info(
                "event=HEALTH_CHECK "
                f"status={result.status} "
                f"modelLoaded={result.modelLoaded} "
                f"modelName={result.modelName} "
                f"device={result.device} "
                f"errorCode={result.errorCode}"
            )
        
        return HealthResponse(
            success=result.success,
            status=result.status,
            modelLoaded=result.modelLoaded,
            modelName=result.modelName,
            vectorDim=result.vectorDim,
            device=result.device,
            errorCode=result.errorCode,
            message=result.message,
        )
    
    except Exception as e:
        logger.exception(f"event=HEALTH_CHECK_FAILED message={str(e)}")

        return HealthResponse(
            success=False,
            status="DOWN",
            modelLoaded=False,
            modelName="",
            vectorDim=0,
            device="",
            errorCode="HEALTH_CHECK_ERROR",
            message=str(e),
        )
        
        
        
        
@router.post("/vectorize/image", response_model=VectorizeResponse)
async def vectorize_image(
    request: Request,
    imageId: int = Form(default=None),
    traceId: str = Form(default=None),
    requestId: str = Form(default=None),
    file: UploadFile = File(...),
):
    service = get_vectorization_service(request)
    
    image_bytes = await file.read()
    
    image_request = ImageVectorizeRequest(
        imageId=imageId,
        traceId=traceId,
        requestId=requestId,
        originalFileName=file.filename,
        mimeType=file.content_type,
        fileSize=len(image_bytes),
    )
    
    logger.info(
        "event=VECTORIZE_IMAGE_REQUEST "
        f"traceId={image_request.traceId} "
        f"requestId={image_request.requestId} "
        f"imageId={image_request.imageId} "
        f"fileName={image_request.originalFileName} "
        f"mimeType={image_request.mimeType} "
        f"fileSize={image_request.fileSize}"
    )
    
    async with inference_lock:
        result = service.vectorize_image(
            image_bytes=image_bytes,
            image_id=image_request.imageId,
            trace_id=image_request.traceId,
            request_id=image_request.requestId,
        )

    return build_vectorize_response(result)


@router.post("/vectorize/text", response_model=VectorizeResponse)
async def vectorize_text(
    request: Request,
    traceId: str = Form(default=None),
    requestId: str = Form(default=None),
    text: str = Form(...)
):
    service = get_vectorization_service(request)

    
    logger.info(
        "event=VECTORIZE_TEXT_REQUEST "
        f"traceId={traceId} "
        f"requestId={requestId} "
        f"textLength={len(text)}"
    )
    
    async with inference_lock:
        result = service.vectorize_text(
            text=text,
            trace_id=traceId,
            request_id=requestId,
        )
    
    return build_vectorize_response(result)