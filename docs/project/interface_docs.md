# EveryPicFound 前端接口文档

> 版本：v1（当前实现）
> 统一入口：`http://localhost:8082`（本地直接启动 Gateway）或 Compose 暴露的 `http://localhost:8080`
> 更新时间：2026-07-18

本文档以当前 Gateway、Identity 和 Media Search 的代码为准，供前端调用。前端只应访问 Gateway；不要直接请求 Identity 或 Media 服务端口。

## 1. 调用约定

### 1.1 身份凭据

登录和刷新成功后会返回 `accessToken` 与 `refreshToken`。除注册、登录、刷新外，受保护接口均需携带：

```http
Authorization: Bearer <accessToken>
```

Access Token 当前有效期为 30 分钟；Refresh Token 当前有效期为 1 小时。当前版本以 JSON 返回和提交 Refresh Token，不使用 Cookie。

收到 `401` 时，前端可先调用刷新接口；刷新失败则清除本地两个 Token 并回到登录页。退出、修改密码和注销成功后也必须立即清除本地两个 Token。

### 1.2 两种响应格式

Identity 接口（`/api/auth/**`、`/api/users/**`）直接返回业务对象；失败时返回：

```json
{
  "errorCode": "AUTH_INVALID_CREDENTIALS",
  "message": "登录凭据无效",
  "field": null
}
```

图片与搜索接口当前沿用 Media 的统一包装：

```json
{
  "code": 0,
  "message": "success",
  "data": {},
  "requestId": "..."
}
```

不要把两种格式混用。HTTP 状态码仍然是判断请求是否成功的第一依据。

### 1.3 权限范围

当前登录用户默认拥有 `image:read`、`image:search`、`image:upload`、`user:read`、`user:write`。Gateway 和 Media 都会校验 Token；无 Token 或无效 Token 返回 `401`，Token 有效但 scope 不足返回 `403`。

## 2. 认证接口

### POST /api/auth/register

注册用户。无需 Token。

```json
{
  "username": "Alice_01",
  "password": "secret123",
  "nickname": "Alice"
}
```

| 字段 | 必填 | 规则 |
| --- | --- | --- |
| `username` | 是 | 3～32 位；区分大小写；仅英文字母、数字与规则内下划线 |
| `password` | 是 | 6～25 个 Unicode 字符；UTF-8 不超过 72 字节；不能含空白或控制字符 |
| `nickname` | 否 | 为空时展示名回退为用户名；最多 32 字符，不能含控制字符或换行符 |

成功：`201 Created`

```json
{
  "userId": 56,
  "username": "Alice_01",
  "nickname": "Alice",
  "displayName": "Alice"
}
```

常见失败：`400`（字段不合法）、`409 USER_USERNAME_ALREADY_EXISTS`。

### POST /api/auth/login

用户名密码登录。无需 Token。

```json
{
  "username": "Alice_01",
  "password": "secret123"
}
```

成功：`200 OK`

```json
{
  "accessToken": "eyJ...",
  "tokenType": "Bearer",
  "expiresAt": "2026-07-18T10:00:00Z",
  "refreshToken": "...",
  "refreshTokenExpiresAt": "2026-07-18T10:30:00Z"
}
```

失败：`401 AUTH_INVALID_CREDENTIALS`。用户名不存在、密码错误与不可登录账户对外使用同一错误，前端不应尝试区分。

### POST /api/auth/refresh

用当前 Refresh Token 换取新的 Access Token 与新的 Refresh Token。无需 Access Token。

```json
{
  "refreshToken": "..."
}
```

成功响应与登录相同。每次成功刷新后，旧 Refresh Token 立即失效；前端必须原子地替换本地两张 Token。

失败：`401 AUTH_INVALID_REFRESH_TOKEN`。

### POST /api/auth/logout

退出当前登录会话。需要 Bearer Token；成功后撤销该 Session 的 Refresh Token。

成功：`204 No Content`。接口可重复调用；前端无论响应结果如何都应清除本地 Token。

限制：已签发的 Access Token 目前仍可能在其自然过期前被 Gateway/Media 接受；这是当前版本已知边界。

## 3. 当前用户接口

以下接口都使用当前 Bearer Token 的 `sub` 识别用户，前端不传 `userId`。

### GET /api/users/me

需要 `user:read`。

成功：`200 OK`

```json
{
  "userId": 56,
  "username": "Alice_01",
  "nickname": "Alice",
  "displayName": "Alice",
  "avatarUrl": "https://example.com/avatar.png"
}
```

`nickname` 为空时，`displayName` 等于 `username`。账户不存在或已不可用时返回 `404 USER_PROFILE_NOT_FOUND`。

### PATCH /api/users/me/profile

需要 `user:write`。两个字段都可选；传空字符串可清空昵称或头像地址。

```json
{
  "nickname": "新的昵称",
  "avatarUrl": "https://example.com/new-avatar.png"
}
```

成功：`200 OK`，响应同 `GET /api/users/me`。

### PATCH /api/users/me/password

需要 `user:write`。

```json
{
  "currentPassword": "secret123",
  "newPassword": "newsecret123"
}
```

成功：`204 No Content`。服务端更新 BCrypt 摘要、推进认证有效时间，并撤销该用户已有的所有 Session 和 Refresh Token。前端成功后必须清除本地 Token 并要求用户重新登录。

失败：当前密码错误返回 `401 AUTH_INVALID_CREDENTIALS`；新密码格式不合法返回 `400`。

### DELETE /api/users/me

需要 `user:write`。请求体不能省略。

```json
{
  "password": "newsecret123"
}
```

成功：`204 No Content`。服务端将账户标记为 `DELETED`，撤销全部 Session/Refresh Token，并释放原用户名供后续重新注册。前端必须清除本地 Token 和用户缓存。

## 4. 图片与搜索接口

### POST /api/images/upload

需要 `image:upload`。`multipart/form-data`。

| 表单字段 | 必填 | 说明 |
| --- | --- | --- |
| `imageFile` | 是 | 图片文件；当前允许 jpg、jpeg、png、webp，最大 10 MB |

成功响应为 Media 包装，`data` 包含 `imageId`、`originalFileName`、`imageUrl`、`imageStatus`、`vectorStatus`。

### GET /images/**

需要 `image:read`。使用上传响应中的 `imageUrl` 访问实际图片二进制，例如：`GET /images/2026/07/18/xxx.jpg`。

### POST /api/search/image

需要 `image:search`。`multipart/form-data`。

| 表单字段 | 必填 | 说明 |
| --- | --- | --- |
| `queryImage` | 是 | 查询图片 |
| `topK` | 否 | 返回数量；当前默认值与上限由后端搜索规则决定 |

### POST /api/search/text

需要 `image:search`。`application/json`。

```json
{
  "queryText": "一只橘猫在窗边晒太阳",
  "topK": 10
}
```

### POST /api/search/hybrid

需要 `image:search`。`multipart/form-data`。

| 表单字段 | 必填 | 说明 |
| --- | --- | --- |
| `queryImage` | 是 | 查询图片 |
| `queryText` | 是 | 文本描述 |
| `topK` | 否 | 返回数量 |

三种搜索成功时均返回 Media 包装，`data` 为搜索结果对象，包含 `searchType`、`total`、`items`、`costMs`。每项通常包含图片 ID、图片 URL、原始文件名、相似度、宽高和 MIME 类型。

## 5. 开发与运维接口

这些接口不作为正式前端功能依赖。

| 方法 | 路径 | 条件 | 说明 |
| --- | --- | --- | --- |
| `GET` | `/actuator/health` | 公开 | Gateway 健康检查 |
| `GET` | `/actuator/info` | 公开 | Gateway 信息 |
| `GET` | `/api/search/_auth/probe` | Media `dev` profile，`image:search` | 返回当前认证主体和权限，仅用于验权链路调试 |
| `GET` | `/dev/modelclient/health` | Media 开发接口 | 模型服务健康检查 |
| `POST` | `/dev/modelclient/vectorize/text` | Media 开发接口 | `application/x-www-form-urlencoded`，字段 `text` |
| `POST` | `/dev/modelclient/vectorize/image` | Media 开发接口 | `multipart/form-data`，字段 `file` |

## 6. Identity 错误码

| HTTP | errorCode | 前端处理建议 |
| --- | --- | --- |
| 400 | `USER_USERNAME_REQUIRED` / `USER_USERNAME_LENGTH_INVALID` / `USER_USERNAME_FORMAT_INVALID` | 标记用户名字段 |
| 400 | `USER_NICKNAME_LENGTH_INVALID` / `USER_NICKNAME_INVALID_CHARACTER` | 标记昵称字段 |
| 400 | `USER_PASSWORD_REQUIRED` / `USER_PASSWORD_LENGTH_INVALID` / `USER_PASSWORD_UTF8_TOO_LONG` / `USER_PASSWORD_WHITESPACE_NOT_ALLOWED` / `USER_PASSWORD_CONTROL_CHARACTER_NOT_ALLOWED` | 标记密码字段 |
| 401 | `AUTH_INVALID_CREDENTIALS` | 登录或当前密码错误提示 |
| 401 | `AUTH_INVALID_REFRESH_TOKEN` | 清除本地 Token，回到登录页 |
| 401 | `AUTH_INVALID_ACCESS_TOKEN` | 清除 Token 或尝试刷新后重试 |
| 404 | `USER_PROFILE_NOT_FOUND` | 清除用户态并返回登录页 |
| 409 | `USER_USERNAME_ALREADY_EXISTS` | 提示更换用户名 |
| 500 | `SYSTEM_INTERNAL_ERROR` | 通用错误提示，保留请求上下文供排查 |

## 7. 接口总览

| 方法 | 路径 | 鉴权 |
| --- | --- | --- |
| POST | `/api/auth/register` | 公开 |
| POST | `/api/auth/login` | 公开 |
| POST | `/api/auth/refresh` | 公开 |
| POST | `/api/auth/logout` | Bearer |
| GET | `/api/users/me` | `user:read` |
| PATCH | `/api/users/me/profile` | `user:write` |
| PATCH | `/api/users/me/password` | `user:write` |
| DELETE | `/api/users/me` | `user:write` |
| POST | `/api/images/upload` | `image:upload` |
| GET | `/images/**` | `image:read` |
| POST | `/api/search/image` | `image:search` |
| POST | `/api/search/text` | `image:search` |
| POST | `/api/search/hybrid` | `image:search` |
