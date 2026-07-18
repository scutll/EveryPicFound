# Python 模型服务架构设计文档

## 1. MVP 部署方案

MVP 阶段 Python 模型服务采用单服务、单模型、单 GPU 部署方式。

```text
Java 后端
  ↓ HTTP
Python FastAPI 模型服务
  ↓
单份图文向量化模型
  ↓
GPU 推理
```

服务启动时加载一份图文向量化模型，并常驻 GPU。当前阶段不部署多份模型，不做模型路由，不做多 GPU 调度。

## 2. 服务接口

Python 模型服务只提供三个接口：

```text
GET  /health
POST /vectorize/image
POST /vectorize/text
```

所有向量化请求必须携带：

```text
traceId
requestId
```

其中：

```text
traceId：完整业务链路 ID，用于串联上传、向量化、向量入库、搜索等流程
requestId：本次 Java 调用 Python 服务的 HTTP 请求 ID
```

## 3. 健康检查接口

```http
GET /health
```

返回示例：

```json
{
  "success": true,
  "status": "UP",
  "modelLoaded": true,
  "modelName": "clip-vit-base-patch32",
  "vectorDim": 512
}
```

| 字段 | 说明 |
|---|---|
| `success` | 接口是否正常返回 |
| `status` | 服务状态，`UP` 或 `DOWN` |
| `modelLoaded` | 模型是否已加载 |
| `modelName` | 当前加载的模型名称 |
| `vectorDim` | 当前模型输出向量维度 |

## 4. 图片向量化接口

```http
POST /vectorize/image
Content-Type: multipart/form-data
```

请求字段：

| 字段 | 是否必填 | 说明 |
|---|---|---|
| `imageId` | 是 | 图片 ID，与 Java 后端 `image_asset.id` 对应 |
| `file` | 是 | 图片文件二进制 |
| `traceId` | 是 | 完整链路追踪 ID |
| `requestId` | 是 | 本次 HTTP 请求 ID |

成功返回：

```json
{
  "success": true,
  "vectorizeType": "IMAGE",
  "imageId": 10001,
  "embedding": [0.12, -0.03, 0.88],
  "dim": 512,
  "modelName": "clip-vit-base-patch32",
  "errorCode": null,
  "message": "success"
}
```

失败返回：

```json
{
  "success": false,
  "vectorizeType": "IMAGE",
  "imageId": 10001,
  "embedding": null,
  "dim": 0,
  "modelName": "clip-vit-base-patch32",
  "errorCode": "IMAGE_DECODE_ERROR",
  "message": "image decode failed"
}
```

## 5. 文本向量化接口

```http
POST /vectorize/text
Content-Type: application/json
```

请求示例：

```json
{
  "text": "海边日落",
  "traceId": "trace-xxx",
  "requestId": "req-xxx"
}
```

请求字段：

| 字段 | 是否必填 | 说明 |
|---|---|---|
| `text` | 是 | 查询文本 |
| `traceId` | 是 | 完整链路追踪 ID |
| `requestId` | 是 | 本次 HTTP 请求 ID |

成功返回：

```json
{
  "success": true,
  "vectorizeType": "TEXT",
  "embedding": [0.15, 0.42, -0.09],
  "dim": 512,
  "modelName": "clip-vit-base-patch32",
  "errorCode": null,
  "message": "success"
}
```

失败返回：

```json
{
  "success": false,
  "vectorizeType": "TEXT",
  "embedding": null,
  "dim": 0,
  "modelName": "clip-vit-base-patch32",
  "errorCode": "TEXT_EMPTY",
  "message": "text is empty"
}
```

## 6. 图片传输方式

MVP 阶段采用 Java 读取图片文件后，通过 `multipart/form-data` 将图片二进制传给 Python 服务。

```text
Java 读取 storagePath 对应图片
  ↓
POST /vectorize/image
  ↓
Python 接收图片二进制
  ↓
图片预处理
  ↓
模型推理
  ↓
返回 embedding
```

Python 服务不直接读取 Java 的本地文件路径。

## 7. 模型加载策略

服务启动时加载模型：

```text
FastAPI 启动
  ↓
加载图文向量化模型
  ↓
移动到 GPU
  ↓
设置 eval 模式
  ↓
对外提供向量化接口
```

模型对象在服务进程内复用，不允许每次请求重新加载模型。

如果模型加载失败：

```text
/health 返回 DOWN
/vectorize/image 返回 MODEL_NOT_LOADED
/vectorize/text 返回 MODEL_NOT_LOADED
```

## 8. 并发处理策略

MVP 阶段采用：

```text
单 worker
单模型
单 GPU
HTTP 请求可以并发进入
模型推理阶段串行执行
```

FastAPI 可以同时接收多个 HTTP 请求，但真正进入 GPU 模型推理时，需要通过进程内异步锁控制：

```text
请求 A 到达
请求 B 到达
  ↓
A 先获得推理锁
  ↓
A 执行模型推理
  ↓
A 释放推理锁
  ↓
B 获得推理锁
  ↓
B 执行模型推理
```

这样保证同一时刻只有一个请求真正使用 GPU 模型，避免小显存服务器出现显存峰值过高、推理资源竞争或 OOM。

Python 侧可使用全局异步锁：

```python
import asyncio

inference_lock = asyncio.Lock()

async def vectorize_image(...):
    async with inference_lock:
        # 图片预处理
        # 模型推理
        # 向量归一化
        return result

async def vectorize_text(...):
    async with inference_lock:
        # 文本预处理
        # 模型推理
        # 向量归一化
        return result
```

`/vectorize/image` 和 `/vectorize/text` 共用同一把锁。

## 9. 启动方式

MVP 阶段使用单 worker 启动：

```bash
uvicorn main:app --host 0.0.0.0 --port 8001 --workers 1
```

不建议使用多个 worker。多个 worker 可能会加载多份模型，导致显存占用翻倍。

## 10. Python 服务目录结构

MVP 目录结构建议如下：

```text
model-service/
  main.py
  config.py
  requirements.txt

  app/
    api.py
    schemas.py
    model_loader.py
    vectorization_service.py
```

| 文件 | 职责 |
|---|---|
| `main.py` | 创建 FastAPI 应用并启动服务 |
| `config.py` | 管理模型名称、设备、向量维度等配置 |
| `api.py` | 定义 `/health`、`/vectorize/image`、`/vectorize/text` |
| `schemas.py` | 定义请求和响应结构 |
| `model_loader.py` | 负责模型加载、设备选择、模型复用 |
| `vectorization_service.py` | 负责图片预处理、文本预处理、模型推理、向量归一化 |

## 11. 错误码

Python 服务 MVP 阶段保留以下错误码：

| 错误码 | 说明 |
|---|---|
| `MODEL_NOT_LOADED` | 模型未加载 |
| `IMAGE_EMPTY` | 图片文件为空 |
| `IMAGE_DECODE_ERROR` | 图片解码失败 |
| `UNSUPPORTED_IMAGE_FORMAT` | 不支持的图片格式 |
| `TEXT_EMPTY` | 文本为空 |
| `MODEL_INFERENCE_ERROR` | 模型推理失败 |
| `INTERNAL_ERROR` | 未预期系统错误 |

Java 后端可将 Python 错误映射为 Java 侧统一错误码：

| Python 错误码 | Java 错误码 |
|---|---|
| `IMAGE_DECODE_ERROR` | `IMAGE_DECODE_ERROR` |
| `TEXT_EMPTY` | `MODEL_SERVICE_ERROR` |
| `MODEL_NOT_LOADED` | `MODEL_SERVICE_ERROR` |
| `MODEL_INFERENCE_ERROR` | `MODEL_SERVICE_ERROR` |
| HTTP 超时 | `MODEL_SERVICE_TIMEOUT` |

## 12. 后续扩展方向

后续如果服务器资源允许，可以扩展为：

```text
多模型实例
多 GPU 部署
请求队列
模型服务负载均衡
批量向量化
模型版本路由
```

MVP 阶段暂不实现。
