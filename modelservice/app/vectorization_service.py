import io
import time
from dataclasses import dataclass
from typing import Optional
from pathlib import Path

import torch
from PIL import Image, UnidentifiedImageError

from app.model_loader import OpenClipRuntime
from app.log_utils import get_model_service_logger
from config import WARMUP_IMAGE_PATH

logger = get_model_service_logger()

@dataclass
class HealthCheckResult:
    success: bool
    status: str
    modelLoaded: bool
    modelName: str
    vectorDim: int
    device: str
    errorCode: Optional[str]
    message: str

@dataclass
class VectorizeResult:
    success: bool
    vectorizeType: str
    embedding: Optional[list[float]]
    dim: int
    modelName: str
    errorCode: Optional[str]
    message: str
    costMs: int
    imageId: Optional[int] = None
    
    
class VectorizationService:
    def __init__(self, runtime: OpenClipRuntime):
        self.runtime = runtime
        
        
    def _check_runtime(self):
        if self.runtime is None:
            raise RuntimeError("model runtime not initialized")
        
        if self.runtime.model is None:
            raise RuntimeError("model not loaded")
        
        if self.runtime.preprocess is None:
            raise RuntimeError("image preprocess not loaded")
        
        if self.runtime.tokenizer is None:
            raise RuntimeError("tokenizer not loaded")
        
    # 等待GPU上的计算任务执行完再往下走，防止异步操作使得向量化计时有误差
    def _sync_cuda(self) -> None:
        if str(self.runtime.device).startswith("cuda"):
            torch.cuda.synchronize()

    @staticmethod
    def _cost_ms(start: float) -> int:
        return int((time.perf_counter() - start) * 1000)
            
    def _fail(
        self,
        vectorize_type:str,
        image_id: Optional[int],
        error_code: str,
        message: str,
        start: float,
        trace_id: Optional[str],
        request_id: Optional[str]
    ) -> VectorizeResult:
        
        cost_ms = self._cost_ms(start)
        
        logger.warning(
            f"event=VECTORIZE_{vectorize_type}_FAILED "
            f"traceId={trace_id} requestId={request_id} imageId={image_id} "
            f"errorCode={error_code} costMs={cost_ms} message={message}"
        )
        
        return VectorizeResult(
            success=False,
            vectorizeType=vectorize_type,
            imageId=image_id,
            embedding=None,
            dim=0,
            modelName=self.runtime.model_name if self.runtime.model else "",
            errorCode=error_code,
            message=message,
            costMs=cost_ms
        )
        
        
    def vectorize_image(
        self,
        image_bytes: bytes,
        image_id: Optional[int] = None,
        trace_id: Optional[str] = None,
        request_id: Optional[str] = None
    ) -> VectorizeResult:
        type="IMAGE"
        start = time.perf_counter()
        
        try:
            self._check_runtime()
            
            if not image_bytes:
                return self._fail(
                    vectorize_type=type,
                    image_id=image_id,
                    error_code="IMAGE_EMPTY",
                    message="image file empty",
                    start=start,
                    trace_id=trace_id,
                    request_id=request_id
                )

            image = Image.open(io.BytesIO(image_bytes)).convert("RGB")
            image_tensor = self.runtime.preprocess(image).unsqueeze(0).to(self.runtime.device)
            
            with torch.inference_mode():
                embedding = self.runtime.model.encode_image(image_tensor)
                embedding = embedding / embedding.norm(dim=-1, keepdim=True)
                
            self._sync_cuda()
            
            vector = embedding[0].detach().cpu().tolist()
            dim = len(vector)
            cost_ms = self._cost_ms(start)
            
            if dim != self.runtime.vector_dim:
                return self._fail(
                    vectorize_type=type,
                    image_id=image_id,
                    error_code="VECTOR_DIM_MISMATCH",
                    message=f"expected dim {self.runtime.vector_dim}, actual dim {dim}",
                    start=start,
                    trace_id=trace_id,
                    request_id=request_id,
                )
                
            logger.info(
                "event=VECTORIZE_IMAGE_SUCCESS "
                f"traceId={trace_id} requestId={request_id} imageId={image_id} "
                f"modelName={self.runtime.model_name} dim={dim} costMs={cost_ms}"
            )
            
            return VectorizeResult(
                success=True,
                vectorizeType=type,
                imageId=image_id,
                embedding=vector,
                dim=dim,
                modelName=self.runtime.model_name,
                errorCode=None,
                message="success",
                costMs=cost_ms,
            )
            
        except UnidentifiedImageError:
            return self._fail(
                vectorize_type=type,
                image_id=image_id,
                error_code="IMAGE_DECODE_ERROR",
                message="image decode failed",
                start=start,
                trace_id=trace_id,
                request_id=request_id,
            )
        
        except Exception as e:
            cost_ms = self._cost_ms(start)
            logger.exception(
                "event=VECTORIZE_IMAGE_FAILED "
                f"traceId={trace_id} requestId={request_id} imageId={image_id} "
                f"errorCode=MODEL_INFERENCE_ERROR costMs={cost_ms} message={str(e)}"
            )

            return VectorizeResult(
                success=False,
                vectorizeType=type,
                imageId=image_id,
                embedding=None,
                dim=0,
                modelName=self.runtime.model_name if self.runtime else "",
                errorCode="MODEL_INFERENCE_ERROR",
                message=str(e),
                costMs=cost_ms,
            )
            
            
    def vectorize_text(
        self,
        text: str,
        trace_id: Optional[str] = None,
        request_id: Optional[str] = None,
    ) -> VectorizeResult:
        type = "TEXT"
        start = time.perf_counter()
        
        try:
            self._check_runtime()
            
            if not text or not text.strip():
                return self._fail(
                    vectorize_type=type,
                    image_id=None,
                    error_code="TEXT_EMPTY",
                    message="text empty",
                    start=start,
                    trace_id=trace_id,
                    request_id=request_id,
                )
                
            tokens = self.runtime.tokenizer([text]).to(self.runtime.device)
            
            with torch.inference_mode():
                embedding = self.runtime.model.encode_text(tokens)
                embedding = embedding / embedding.norm(dim=-1, keepdim=True)
                
            self._sync_cuda()
            
            
            vector = embedding[0].detach().cpu().tolist()
            dim = len(vector)
            cost_ms = self._cost_ms(start)
            
            if dim != self.runtime.vector_dim:
                return self._fail(
                    vectorize_type=type,
                    image_id=None,
                    error_code="VECTOR_DIM_MISMATCH",
                    message=f"expected dim {self.runtime.vector_dim}, actual dim {dim}",
                    start=start,
                    trace_id=trace_id,
                    request_id=request_id,
                )

            logger.info(
                "event=VECTORIZE_TEXT_SUCCESS "
                f"traceId={trace_id} requestId={request_id} "
                f"modelName={self.runtime.model_name} dim={dim} costMs={cost_ms} "
                f"textLength={len(text)}"
            )
            
            return VectorizeResult(
                success=True,
                vectorizeType=type,
                imageId=None,
                embedding=vector,
                dim=dim,
                modelName=self.runtime.model_name,
                errorCode=None,
                message="success",
                costMs=cost_ms,
            )

        except Exception as e:
            cost_ms = self._cost_ms(start)
            logger.exception(
                "event=VECTORIZE_TEXT_FAILED "
                f"traceId={trace_id} requestId={request_id} "
                f"errorCode=MODEL_INFERENCE_ERROR costMs={cost_ms} message={str(e)}"
            )

            return VectorizeResult(
                success=False,
                vectorizeType="TEXT",
                imageId=None,
                embedding=None,
                dim=0,
                modelName=self.runtime.model_name if self.runtime else "",
                errorCode="MODEL_INFERENCE_ERROR",
                message=str(e),
                costMs=cost_ms,
            )
            
         
        
    # 设置一个warm_up来在初次加载模型之后先执行一次向量化任务来初始化CUDA等配置，后面的向量化请求速度就会保持高速
    def warm_up(self):
        print("模型正在warm up: ")

        self.check_health()

        image_path = WARMUP_IMAGE_PATH
        image_bytes = image_path.read_bytes()
        # warm up
        self.vectorize_image(
            image_bytes
        )

        self._sync_cuda()

        print("模型 warm up 完成!")


        
    def check_health(self) -> HealthCheckResult:
        try:
            if self.runtime is None:
                return HealthCheckResult(
                    success=False,
                    status="DOWN",
                    modelLoaded=False,
                    modelName="",
                    vectorDim=0,
                    device="",
                    errorCode="MODEL_RUNTIME_NOT_INITIALIZED",
                    message="model runtime is not initialized",
                )

            if self.runtime.model is None:
                return HealthCheckResult(
                    success=False,
                    status="DOWN",
                    modelLoaded=False,
                    modelName=self.runtime.model_name,
                    vectorDim=self.runtime.vector_dim,
                    device=self.runtime.device,
                    errorCode="MODEL_NOT_LOADED",
                    message="model is not loaded",
                )

            if self.runtime.preprocess is None:
                return HealthCheckResult(
                    success=False,
                    status="DOWN",
                    modelLoaded=False,
                    modelName=self.runtime.model_name,
                    vectorDim=self.runtime.vector_dim,
                    device=self.runtime.device,
                    errorCode="IMAGE_PREPROCESS_NOT_LOADED",
                    message="image preprocess is not loaded",
                )

            if self.runtime.tokenizer is None:
                return HealthCheckResult(
                    success=False,
                    status="DOWN",
                    modelLoaded=False,
                    modelName=self.runtime.model_name,
                    vectorDim=self.runtime.vector_dim,
                    device=self.runtime.device,
                    errorCode="TEXT_TOKENIZER_NOT_LOADED",
                    message="text tokenizer is not loaded",
                )

            return HealthCheckResult(
                success=True,
                status="UP",
                modelLoaded=True,
                modelName=self.runtime.model_name,
                vectorDim=self.runtime.vector_dim,
                device=self.runtime.device,
                errorCode=None,
                message="success",
            )

        except Exception as e:
            logger.exception(f"event=HEALTH_CHECK_FAILED message={str(e)}")

            return HealthCheckResult(
                success=False,
                status="DOWN",
                modelLoaded=False,
                modelName="",
                vectorDim=0,
                device="",
                errorCode="HEALTH_CHECK_ERROR",
                message=str(e),
            )