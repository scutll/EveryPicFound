# EveryPicFound 接口文档

> 版本: v1.0  
> 更新日期: 2026-06-11  
> 基础地址: `http://localhost:8080`

---

## 目录

1. [通用说明](#1-通用说明)
2. [图片上传](#2-图片上传)
3. [图片文件访问](#3-图片文件访问)
4. [图片搜索](#4-图片搜索)
5. [Dev 测试接口](#5-dev-测试接口)
6. [附录](#6-附录)

---

## 1. 通用说明

### 1.1 统一响应结构

所有接口返回的 HTTP 状态码均为 `200`，业务结果通过响应体中的 `code` 字段区分。

```json
{
  "code": 0,
  "message": "success",
  "data": { ... },
  "requestId": "req-xxx"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `code` | `Integer` | 业务状态码。`0` 表示成功，`2xxxxx` 为 image-asset 模块错误，其他见附录 |
| `message` | `String` | 状态描述 |
| `data` | `Object` | 业务数据，失败时为 `null` |
| `requestId` | `String` | 请求追踪 ID |

### 1.2 图片格式限制

| 项目 | 限制 |
|------|------|
| 允许扩展名 | `jpg`, `jpeg`, `png`, `webp` |
| 最大文件大小 | 10 MB |

---

## 2. 图片上传

### POST /api/images/upload

上传一张图片，系统会自动完成校验、去重、元数据解析、文件落盘、数据库入库，并触发向量化任务。

**Content-Type:** `multipart/form-data`

**请求参数:**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `imageFile` | `File` | 是 | 图片文件 |

**请求示例 (curl):**

```bash
curl -X POST http://localhost:8080/api/images/upload \
  -F "imageFile=@/path/to/photo.jpg"
```

**请求示例 (Python):**

```python
import requests

with open("photo.jpg", "rb") as f:
    resp = requests.post(
        "http://localhost:8080/api/images/upload",
        files={"imageFile": ("photo.jpg", f, "image/jpeg")}
    )
print(resp.json())
```

**成功响应:**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "imageId": 1234567890,
    "originalFileName": "photo.jpg",
    "imageUrl": "/images/2026/06/11/1234567890.jpg",
    "imageStatus": "NORMAL",
    "vectorStatus": "PENDING"
  },
  "requestId": "req-xxx"
}
```

**响应字段说明:**

| 字段 | 类型 | 说明 |
|------|------|------|
| `data.imageId` | `Long` | 图片唯一 ID（雪花 ID） |
| `data.originalFileName` | `String` | 用户上传时的原始文件名 |
| `data.imageUrl` | `String` | 图片访问地址（相对路径），可通过 `/images/**` 接口访问 |
| `data.imageStatus` | `String` | 图片状态，见 [附录-图片状态](#61-图片状态) |
| `data.vectorStatus` | `String` | 向量状态，见 [附录-向量状态](#62-向量状态) |

**常见错误:**

| code | message | 说明 |
|------|---------|------|
| `200001` | image empty | 上传文件为空 |
| `200002` | image size exceeded limit | 文件超过 10MB |
| `200003` | image format unsupported | 扩展名不支持 |
| `200004` | image mime type invalid | MIME 类型不合法 |
| `200005` | failed to decode image | 图片无法解析 |
| `200006` | image exists already | 图片已存在（SHA-256 去重） |

---

## 3. 图片文件访问

### GET /images/**

根据图片 URL 获取图片文件内容，支持浏览器直接访问和内嵌展示。

**请求方式:** `GET`

**路径说明:**

- 上传成功后返回的 `imageUrl` 字段即为相对路径，例如 `/images/2026/06/11/1234567890.jpg`
- 拼接基础地址即为完整 URL: `http://localhost:8080/images/2026/06/11/1234567890.jpg`

**请求示例:**

```bash
curl http://localhost:8080/images/2026/06/11/1234567890.jpg -o output.jpg
```

```html
<img src="http://localhost:8080/images/2026/06/11/1234567890.jpg" />
```

**响应:**

| 状态码 | 说明 |
|--------|------|
| `200` | 返回图片二进制流，`Content-Type` 为实际 MIME 类型，`Content-Length` 为文件大小 |
| 异常 | 路径非法（含 `..` 遍历）或文件不存在时，返回统一错误 JSON |

---

## 4. 图片搜索

搜索模块提供三种搜索方式，均返回统一结构的 `SearchResponse`。

**通用搜索响应结构:**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "searchType": "IMAGE",
    "total": 10,
    "items": [
      {
        "imageId": 1234567890,
        "imageUrl": "/images/2026/06/11/1234567890.jpg",
        "fileName": "1234567890.jpg",
        "originalFileName": "photo.jpg",
        "score": 0.95,
        "width": 1920,
        "height": 1080,
        "mimeType": "image/jpeg"
      }
    ],
    "costMs": 150
  },
  "requestId": "req-xxx"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `searchType` | `String` | 搜索类型: `IMAGE` / `TEXT` / `HYBRID` |
| `total` | `Integer` | 本次召回结果数量 |
| `items[].imageId` | `Long` | 图片 ID |
| `items[].imageUrl` | `String` | 图片访问地址 |
| `items[].fileName` | `String` | 系统生成的文件名 |
| `items[].originalFileName` | `String` | 用户上传时的原始文件名 |
| `items[].score` | `Float` | 相似度得分，范围 0~1 |
| `items[].width` | `Integer` | 图片宽度 (px) |
| `items[].height` | `Integer` | 图片高度 (px) |
| `items[].mimeType` | `String` | 图片 MIME 类型 |
| `costMs` | `Long` | 搜索耗时（毫秒） |

---

### 4.1 以图搜图

### POST /api/search/image

上传一张图片作为查询，召回视觉相似的图片。

**Content-Type:** `multipart/form-data`

**请求参数:**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `queryImage` | `File` | 是 | 查询图片文件 |
| `topK` | `Integer` | 否 | 返回结果数量，默认 `30`，最大 `50` |

**请求示例:**

```bash
curl -X POST http://localhost:8080/api/search/image \
  -F "queryImage=@query.jpg" \
  -F "topK=10"
```

---

### 4.2 以文搜图

### POST /api/search/text

输入文本描述，召回匹配的图片。

**Content-Type:** `application/json`

**请求参数:**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `queryText` | `String` | 是 | 搜索文本，最长 500 字符 |
| `topK` | `Integer` | 否 | 返回结果数量，默认 `30`，最大 `50` |

**请求示例:**

```bash
curl -X POST http://localhost:8080/api/search/text \
  -H "Content-Type: application/json" \
  -d '{"queryText": "一只橘猫在窗边晒太阳", "topK": 10}'
```

---

### 4.3 图文联合搜图

### POST /api/search/hybrid

同时使用图片和文本进行联合搜索，后端按配置权重融合两个向量后召回结果。

**Content-Type:** `multipart/form-data`

**请求参数:**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `queryImage` | `File` | 是 | 查询图片文件 |
| `queryText` | `String` | 是 | 搜索文本 |
| `topK` | `Integer` | 否 | 返回结果数量，默认 `30`，最大 `50` |

**请求示例:**

```bash
curl -X POST http://localhost:8080/api/search/hybrid \
  -F "queryImage=@query.jpg" \
  -F "queryText=类似这个但更亮一些的图片" \
  -F "topK=10"
```

---

## 5. Dev 测试接口

> ⚠️ 以下接口仅在 `dev` 环境下可用（`@Profile("dev")`），生产环境不开放。

### 5.1 模型服务健康检查

### GET /dev/modelclient/health

检查 Python 模型服务是否可用。

**响应:**

```json
{
  "code": 0,
  "data": {
    "success": true,
    "status": "UP",
    "modelLoaded": true,
    "modelName": "clip",
    "vectorDim": 512,
    "device": "cpu",
    "errorCode": null,
    "message": null
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `success` | `Boolean` | 是否可用 |
| `status` | `String` | 服务状态: `UP` / `DOWN` |
| `modelLoaded` | `Boolean` | 模型是否已加载 |
| `modelName` | `String` | 当前加载的模型名称 |
| `vectorDim` | `Integer` | 向量维度 |
| `device` | `String` | 推理设备: `cpu` / `cuda` |
| `errorCode` | `String` | 失败时的错误码 |

---

### 5.2 文本向量化测试

### POST /dev/modelclient/vectorize/text

**Content-Type:** `application/x-www-form-urlencoded`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `text` | `String` | 是 | 待向量化的文本 |

```bash
curl -X POST "http://localhost:8080/dev/modelclient/vectorize/text" \
  -d "text=a red car"
```

---

### 5.3 图片向量化测试

### POST /dev/modelclient/vectorize/image

**Content-Type:** `multipart/form-data`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `file` | `File` | 是 | 待向量化的图片文件 |

```bash
curl -X POST http://localhost:8080/dev/modelclient/vectorize/image \
  -F "file=@test.jpg"
```

---

## 6. 附录

### 6.1 图片状态 (ImageStatus)

| 值 | 枚举名 | 说明 |
|----|--------|------|
| `1` | `NORMAL` | 正常，图片可展示 |
| `2` | `DELETED` | 已逻辑删除 |
| `3` | `INVALID` | 异常（文件缺失或不可读） |

### 6.2 向量状态 (VectorStatus)

| 值 | 枚举名 | 说明 |
|----|--------|------|
| `1` | `PENDING` | 待向量化 |
| `2` | `PROCESSING` | 向量化处理中 |
| `3` | `READY` | 向量化完成，可搜索 |
| `4` | `FAILED` | 向量化失败 |

### 6.3 搜索类型 (SearchType)

| 值 | 说明 |
|----|------|
| `IMAGE` | 以图搜图 |
| `TEXT` | 以文搜图 |
| `HYBRID` | 图文联合搜索 |

### 6.4 image-asset 模块错误码

| code | 枚举名 | 说明 |
|------|--------|------|
| `200001` | `IMAGE_EMPTY` | 上传图片为空 |
| `200002` | `IMAGE_SIZE_EXCEEDED` | 图片大小超过限制 |
| `200003` | `IMAGE_FORMAT_UNSUPPORTED` | 图片格式不支持 |
| `200004` | `IMAGE_MIME_INVALID` | MIME 类型不合法 |
| `200005` | `IMAGE_DECODE_FAILED` | 图片无法解析 |
| `200006` | `DUPLICATE_IMAGE` | 图片已存在 |
| `200007` | `IMAGE_METADATA_SAVE_FAILED` | 图片元数据入库失败 |
| `200008` | `ORPHAN_FILE_DELETE_FAILED` | 孤儿文件补偿删除失败 |

### 6.5 接口总览

| 接口 | 方法 | Content-Type | 所属模块 | 环境 |
|------|------|-------------|----------|------|
| `/api/images/upload` | `POST` | `multipart/form-data` | image-asset | 全部 |
| `/images/**` | `GET` | — | image-asset | 全部 |
| `/api/search/image` | `POST` | `multipart/form-data` | search | 全部 |
| `/api/search/text` | `POST` | `application/json` | search | 全部 |
| `/api/search/hybrid` | `POST` | `multipart/form-data` | search | 全部 |
| `/dev/modelclient/health` | `GET` | — | model-client | dev only |
| `/dev/modelclient/vectorize/text` | `POST` | `application/x-www-form-urlencoded` | model-client | dev only |
| `/dev/modelclient/vectorize/image` | `POST` | `multipart/form-data` | model-client | dev only |
