from pydantic import BaseModel, Field
from typing import Optional


# 相应包
class HealthResponse(BaseModel):
    success: bool
    status: str
    modelLoaded: bool
    modelName: str
    vectorDim: int
    device: str
    errorCode: Optional[str] = None
    message: str = "success"
    
    
    
class VectorizeResponse(BaseModel):
    success: bool
    vectorizeType: str
    embedding: Optional[list[float]]
    dim: int
    modelName: str
    errorCode: Optional[str]
    message: str
    costMs: int
    imageId: Optional[int] = None
    

# 向量化请求包
class TextVectorizeRequest(BaseModel):
    text: str = Field(..., min_length=1, description="待向量化文本")
    traceId: str = Field(..., description="trace ID")
    requestId: str = Field(..., description="Http request Id")
    

# 图片向量化请求体中不包含图片流数据，图片二进制数据另用multipart放置
class ImageVectorizeRequest(BaseModel):
    imageId: Optional[int] = Field(default=None, description="image Id")
    traceId: str = Field(..., description="trace ID")
    requestId: str = Field(..., description="Http request Id")
    
    originalFileName: Optional[str] = Field(default=None, description="原始文件名")
    mimeType: Optional[str] = Field(default=None, description="MIME 类型")
    fileSize: Optional[int] = Field(default=None, description="文件大小，单位 byte")