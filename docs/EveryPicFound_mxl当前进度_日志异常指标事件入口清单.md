# EveryPicFound 日志、异常、指标与事件入口现状及剩余改造清单

> 基准分支：`mxl`  
> 基准提交：`1f0f261828d6f10a370396e2d560ecae936656c0`  
> 文档性质：当前代码快照 + 剩余施工清单  
> 适用范围：以该提交为基础继续进行日志、异常、指标和事件入口迁移  
> 替代文档：  
> - `EveryPicFound 核心业务链路日志、异常与指标记录入口清单.md`  
> - `EveryPicFound 日志指标入口改造对照清单.md`  
> - 之前生成的 `EveryPicFound_日志异常指标事件记录入口最终清单.md`

---

## 1. 文档目标

本文档不再保留历史讨论中的“待改造”“建议新增”或已经完成但仍被标记为未完成的条目。

本文档只回答四个问题：

1. `mxl` 当前代码已经完成了什么；
2. 当前源码中还存在哪些真实的编译阻断和职责错误；
3. 每一个剩余入口最终应该记录什么指标、错误日志或事件日志；
4. 后续应按什么顺序逐入口完成迁移。

后续开发以本文档为唯一施工清单。完成一个入口后，直接更新本文件中的状态，不再维护另一份历史对照表。

---

## 2. 状态标记

| 标记 | 含义 |
| --- | --- |
| `✅ 已完成` | 当前分支已经符合最终设计，不再列入后续施工 |
| `❌ 阻断` | 当前代码引用了已删除接口、枚举或存在明确契约冲突，需要优先处理 |
| `🔧 待完成` | 当前代码可以表达业务流程，但尚未接入最终指标、事件或异常职责 |
| `⏸ 暂不处理` | 当前迭代明确排除，不进入本轮施工 |

“待完成”只表示当前提交中确实尚未完成的内容，不表示历史计划。

---

## 3. 当前已完成的公共基线

以下能力已经落地，不再作为后续逐入口改造任务重复出现。

### 3.1 统一日志门面

文件：

```text
everypicfound-backend/src/main/java/com/everypicfound/common/log/LogService.java
```

当前接口已经收敛为：

```java
void recordError(LogContext context, Throwable throwable);
void recordError(LogContext context);
void recordEvent(LogContext context);
```

当前结论：

- 系统错误使用 `recordError`；
- 无 Throwable 的明确系统失败使用 `recordError(context)`；
- 业务拒绝、重试、降级、状态迁移和待补偿使用 `recordEvent`；
- `recordBizLog`、`recordSuccessLog`、`recordErrorLog` 等旧方法已不属于当前接口。

状态：`✅ 已完成`

---

### 3.2 全局同步异常收口

文件：

```text
everypicfound-backend/src/main/java/com/everypicfound/common/exception/GlobalExceptionHandler.java
```

当前已经实现：

| 异常 | 日志入口 | 事件名 | Throwable |
| --- | --- | --- | --- |
| `BizException` | `recordEvent` | `BUSINESS_REQUEST_REJECTED` | 不打印 |
| `SystemException` | `recordError` | `SYSTEM_EXCEPTION_OCCURRED` | 完整打印 |
| 未知 `Exception` | `recordError` | `COMMON_UNHANDLED_EXCEPTION` | 完整打印 |

同时已经具备结构化日志系统失败时的 fallback Logger 保护。

状态：`✅ 已完成`

后续同步 HTTP 链路中的 Controller、Application Service、Domain Service、Repository 和 Adapter 都不得记录同一异常后继续抛出。

---

### 3.3 指标公共契约

当前已经完成：

```text
MetricRecorder
MicrometerMetricRecorder
NoOpMetricRecorder
MetricName
MetricTag
MetricTags
MetricType
```

`MetricType` 已包含：

```text
COUNTER
TIMER
DISTRIBUTION_SUMMARY
GAUGE
```

当前 `MetricName` 已覆盖以下指标域：

1. 图片上传；
2. 图片文件访问；
3. 搜索；
4. 向量化发布与执行；
5. Storage；
6. ImageAsset Repository；
7. 业务 Cache；
8. Redis；
9. Java Model Client；
10. Qdrant；
11. Active Collection；
12. 系统健康检查；
13. 业务线程池。

当前 `MetricTag` 已包含：

```text
module
operation
result
status
stage
search_type
vectorize_type
source
dependency
cache_name
cache_result
component
executor
endpoint
reason
```

状态：`✅ 已完成`

本轮原则：先使用现有指标和标签。迁移过程中确实发现无法表达的场景时，再补充公共契约。

---

### 3.4 日志事件枚举

当前 `LogEventName` 已包含：

```text
COMMON_UNHANDLED_EXCEPTION
SYSTEM_EXCEPTION_OCCURRED
BUSINESS_REQUEST_REJECTED
ORPHAN_FILE_DETECTED
TASK_PUBLISH_FAILED
VECTORIZATION_RETRY_SCHEDULED
VECTORIZATION_DEAD_FAILED
IMAGE_FILE_MISSING
CACHE_DEGRADED
VECTOR_READY_COMPENSATION_REQUIRED
SYSTEM_COMPONENT_DOWN
SYSTEM_COMPONENT_RECOVERED
STARTUP_HEALTH_CHECK_FAILED
```

状态：`✅ 已完成`

不增加普通成功事件。

---

### 3.5 HTTP 上下文主体能力

`RequestContextFilter` 当前已经完成：

- requestId、traceId 读取或生成；
- `RequestContextHolder` 安装；
- MDC 安装；
- 响应头回传；
- finally 清理。

`ContextAwareTaskDecorator` 当前已经完成：

- RequestContext 快照复制；
- MDC 快照复制；
- 工作线程安装；
- finally 恢复；
- 防止线程复用污染。

状态：主体能力 `✅ 已完成`

仍存在两个非业务观测硬化项，见 `C-01`、`C-02`。

---

## 4. 当前静态检查结论

### 4.1 当前分支处于接口迁移中间态

公共接口已经更新，但多个业务类仍然引用已经不存在的旧方法或旧枚举。

因此当前源码静态上存在编译阻断，包括但不限于：

| 文件 | 当前仍引用的旧内容 |
| --- | --- |
| `DefaultSearchApplicationService` | `recordBizLog`、`recordSuccessLog`、`recordErrorLog` |
| `DefaultImageAssetApplicationService` | `recordBizLog`、`recordSuccessLog`、`recordErrorLog`、`UPLOAD_COST_MS` |
| `LocalFileStorageService` | 旧 LogService 方法、`FILE_*` MetricName、`FILE_*` LogEventName |
| `RedisCacheService` | `recordErrorLog`、`CACHE_GET_FAILED` 等旧事件 |
| `LogOrphanFileLogService` | `recordErrorLog`、`ORPHAN_FILE_RECORD` |

这些不是历史记录，而是当前 `mxl@1f0f261` 代码中的真实引用。

### 4.2 当前优先级

必须先完成：

```text
旧接口引用清理
→ Cache / Redis
→ Storage / Repository
→ Model / Qdrant
→ Search
→ Publisher / Upload / Vectorization
→ Image Access
→ Health
→ Python modelservice
```

不能先在上层继续堆叠新调用，否则同一异常和同一指标会反复调整。

---

## 5. 全局记录规则

### 5.1 日志与指标边界

指标记录：

- 请求或任务次数；
- success、rejected、failed、skipped、degraded；
- 总耗时和阶段耗时；
- 文件大小、文本长度、结果数量；
- Cache hit、miss、invalid、error；
- 线程池当前状态；
- 健康组件当前状态。

业务日志只记录：

- 最终系统错误；
- 业务拒绝；
- 重试；
- 降级；
- 失效；
- 状态迁移；
- 待补偿。

普通成功不记录业务日志。

---

### 5.2 唯一错误日志责任

| 场景 | 唯一错误日志责任点 |
| --- | --- |
| 同步 HTTP 上传、搜索、普通图片读取 | `GlobalExceptionHandler` |
| `StreamingResponseBody` 异步写出失败 | 流式写出 lambda |
| 缓存异常被捕获并降级 | 对应业务 Cache Wrapper |
| 上传删除补偿失败 | `compensateSavedFile` 补偿边界 |
| 任务发布失败并转换为失败结果 | `ThreadPoolVectorizationTaskPublisher` |
| 异步向量化异常 | `DefaultImageVectorizationProcessor` |
| 向量化重试、文件失效、最终失败 | `VectorizationFailureHandler` 只记事件 |
| 健康状态变化 | `SystemHealthCheckService` |
| Python 推理异常 | `VectorizationService` 或 Python 统一异常收口 |

Adapter 或 Repository 如果继续抛出异常，不记录错误日志。

---

### 5.3 固定标签约束

同一个 `MetricName` 每次注册必须使用同一组标签键。

禁止指标标签：

```text
requestId
traceId
imageId
storagePath
fileName
originalFileName
queryText
collectionName
原始 Redis key
异常 message
完整 URL
```

这些字段只能进入日志上下文。

---

## 6. 当前编译与配置阻断

### CFG-01 Prometheus 抓取路径拼写

状态：`❌ 阻断`

文件：

```text
everypicfound-backend/deploy/prometheus/prometheus.yml
```

当前配置：

```yaml
metrics_paht: /actuator/prometheus
```

应改为：

```yaml
metrics_path: /actuator/prometheus
```

验收：

- Prometheus Targets 中后端实例为 `UP`；
- `/actuator/prometheus` 可以被正常抓取；
- 不以“Prometheus 容器启动成功”代替抓取成功验证。

---

### C-01 RequestContextFilter fallback 硬化

状态：`🔧 待完成`

文件：

```text
common/filter/RequestContextFilter.java
```

当前已经完成正常上下文安装和清理。

当前缺口：

- requestId/traceId 生成；
- RequestContext 安装；
- MDC 安装；

上述框架机制本身发生异常时，没有 fallback Logger。

要求：

- 正常请求不记日志；
- 只在上下文基础设施自身失败时使用类级 fallback Logger；
- finally 仍必须清理 RequestContext 和 MDC；
- 不在该 Filter 记录业务异常。

---

### C-02 ContextAwareTaskDecorator fallback 硬化

状态：`🔧 待完成`

文件：

```text
common/executor/ContextAwareTaskDecorator.java
```

当前核心上下文传播已经完成。

当前缺口：

- 上下文安装或恢复机制自身异常时缺少 fallback Logger；
- `Objects.requireNonNull` 的提示文本存在拼写错误。

要求：

- 业务 Runnable 抛出的异常不能被 Decorator 吞掉或记录；
- 只有 install/restore 基础设施失败才使用 fallback Logger；
- 任务结束后继续保证上下文恢复。

---

## 7. Cache 与 Redis

### CR-01 RedisCacheService.get

状态：`❌ 阻断`

文件：

```text
common/cache/RedisCacheService.java
```

当前问题：

- 使用已删除的 `recordErrorLog`；
- 使用当前 `LogEventName` 中不存在的 `CACHE_GET_FAILED`；
- Redis 异常和反序列化异常被吞掉并返回 `null`；
- `null` 同时表示 MISS 和 ERROR；
- Adapter 越权承担业务错误日志；
- 没有 Redis 指标；
- 日志消息包含 key 和原始异常 message。

最终职责：

指标：

```text
REDIS_OPERATIONS
operation=get
result=success|miss|deserialize_failed|failed
```

```text
REDIS_DURATION
operation=get
result=success|miss|deserialize_failed|failed
```

行为：

- MISS 返回 `null`；
- Redis/DataAccessException 抛出 `SystemException` 并保留 cause；
- 反序列化失败可先尝试删除损坏值，然后抛出 `SystemException`；
- Adapter 不记录业务错误日志；
- 不记录原始 Redis key。

验收：

- Wrapper 能明确区分 MISS 和 ERROR；
- Redis 不可用时由 Wrapper 降级，不能在 Adapter 直接伪装为 MISS。

---

### CR-02 RedisCacheService.put

状态：`❌ 阻断`

当前问题：

- 序列化和 Redis 写入异常被吞掉；
- 使用旧日志方法和旧事件；
- 无指标。

最终指标：

```text
REDIS_OPERATIONS
operation=put
result=success|serialize_failed|failed
```

```text
REDIS_DURATION
operation=put
result=success|serialize_failed|failed
```

最终行为：

- 序列化或 Redis 写入异常抛出 `SystemException`；
- 保留原始 cause；
- 不在 Adapter 记录业务日志。

---

### CR-03 RedisCacheService.evict

状态：`❌ 阻断`

最终指标：

```text
REDIS_OPERATIONS
operation=evict
result=success|failed
```

```text
REDIS_DURATION
operation=evict
result=success|failed
```

最终行为：

- 失败抛出 `SystemException`；
- 不吞异常；
- 不记录业务日志。

---

### CR-04 RedisCacheService.exists

状态：`❌ 阻断`

当前问题：

- Redis 异常被转换成 `false`，无法区分 key 不存在和 Redis 不可用。

最终指标：

```text
REDIS_OPERATIONS
operation=exists
result=present|absent|failed
```

```text
REDIS_DURATION
operation=exists
result=present|absent|failed
```

最终行为：

- 正常不存在返回 `false`；
- Redis 异常抛出 `SystemException`；
- 不记录业务日志。

---

### CW-01 DefaultSearchResultCacheService.get

状态：`🔧 待完成`

文件：

```text
search/domain/cache/DefaultSearchResultCacheService.java
```

当前问题：

- 使用裸 `log.warn`；
- 无 hit、miss、error、skipped 指标；
- `null` 无法向搜索用例传递 cache_result；
- 捕获所有 RuntimeException，错误分类过宽。

最终指标：

```text
CACHE_OPERATIONS
cache_name=search_result
operation=get
result=hit|miss|error|skipped
```

```text
CACHE_DURATION
cache_name=search_result
operation=get
result=hit|miss|error
```

最终日志：

Redis 或序列化异常被捕获并降级时：

```text
recordError(context, throwable)
recordEvent(CACHE_DEGRADED)
```

要求：

- hit、miss、skipped 不记日志；
- 降级后搜索继续；
- 不输出 queryText 和原始 key；
- 需要通过返回上下文或其他低耦合方式把 `cache_result` 传给搜索用例指标。

---

### CW-02 DefaultSearchResultCacheService.put / evictByText

状态：`🔧 待完成`

最终指标：

```text
CACHE_OPERATIONS
cache_name=search_result
operation=put|evict
result=success|error|skipped
```

```text
CACHE_DURATION
cache_name=search_result
operation=put|evict
result=success|error
```

规则：

- 空结果不缓存时记录 `skipped`，不记日志；
- 缓存关闭或参数不适用记录 `skipped`；
- Redis 失败记录错误和 `CACHE_DEGRADED`，但不影响主搜索结果。

---

### CW-03 DefaultTextVectorCacheService.get

状态：`🔧 待完成`

文件：

```text
vectorization/domain/cache/DefaultTextVectorCacheService.java
```

当前问题：

- 使用裸 `log.warn`；
- 无 hit、miss、invalid、error、skipped 指标；
- 缓存向量维度非法时仅返回 null，没有清理损坏值；
- 无降级事件。

最终指标：

```text
CACHE_OPERATIONS
cache_name=text_vector
operation=get
result=hit|miss|invalid|error|skipped
```

```text
CACHE_DURATION
cache_name=text_vector
operation=get
result=hit|miss|invalid|error
```

规则：

- invalid 先记录指标，再尝试 evict；
- invalid 本身通常不记 ERROR；
- evict 或 Redis 失败时记录错误和 `CACHE_DEGRADED`；
- 文本模型调用继续执行。

---

### CW-04 DefaultTextVectorCacheService.put / evict

状态：`🔧 待完成`

最终指标：

```text
CACHE_OPERATIONS
cache_name=text_vector
operation=put|evict
result=success|error|skipped
```

```text
CACHE_DURATION
cache_name=text_vector
operation=put|evict
result=success|error
```

规则：

- 参数或 embedding 不可缓存时为 skipped；
- Redis 写入或删除失败不能中断搜索；
- 错误和降级事件只在 Wrapper 记录一次。

---

## 8. Storage 与 Repository

### ST-01 LocalFileStorageService.save

状态：`❌ 阻断`

文件：

```text
storage/infrastructure/local/LocalFileStorageService.java
```

当前问题：

- 使用当前不存在的 `FILE_SAVE_DURATION_MS`、`STORED_FILE_SIZE_BYTES`、`FILE_SAVE_FAILED_COUNT`；
- 使用当前不存在的文件成功/失败事件名；
- 使用旧 LogService 方法；
- 保存成功写业务日志；
- 失败记录后继续抛出，导致同步链路重复日志。

最终指标：

```text
STORAGE_OPERATIONS
operation=save
result=success|failed
```

```text
STORAGE_DURATION
operation=save
result=success|failed
```

```text
STORAGE_FILE_SIZE
operation=save
```

最终行为：

- 成功不记日志；
- IOException 转换成 `SystemException` 并保留 cause；
- Storage 不记录后继续抛出的错误；
- 上传链路最终由 `GlobalExceptionHandler` 记录一次。

---

### ST-02 LocalFileStorageService.read

状态：`❌ 阻断`

最终指标：

```text
STORAGE_OPERATIONS
operation=read
result=success|not_found|failed
```

```text
STORAGE_DURATION
operation=read
result=success|not_found|failed
```

```text
STORAGE_MISSING_FILES
source=image_access|vectorization
```

说明：

- `source` 不是 Storage 本身天然知道的字段；
- 如果不希望修改 Storage API，`STORAGE_MISSING_FILES` 可由调用编排点记录；
- Storage 只记录 operation/result。

最终行为：

- 文件不存在抛出明确 `SystemException(FILE_NOT_FOUND)`；
- IOException 保留 cause；
- 不记成功日志；
- 不在同步链路重复记录失败。

---

### ST-03 LocalFileStorageService.delete

状态：`❌ 阻断`

当前问题：

- IOException 被记录后返回 false；
- FILE_NOT_FOUND、删除失败、正常 false 契约混合；
- 使用旧日志和旧指标。

最终指标：

```text
STORAGE_OPERATIONS
operation=delete
result=success|not_found|failed
```

```text
STORAGE_DURATION
operation=delete
result=success|not_found|failed
```

最终行为：

- 删除不存在返回 `false` 或抛出 FILE_NOT_FOUND，必须统一一种契约；
- 推荐：不存在返回 `false`，系统 IOException 抛 `SystemException`；
- 补偿调用方根据 false 判断孤儿文件；
- Storage 本身不记录业务错误日志。

---

### ST-04 LocalFileStorageService.exists

状态：`🔧 待完成`

最终指标：

```text
STORAGE_OPERATIONS
operation=exists
result=present|absent|failed
```

```text
STORAGE_DURATION
operation=exists
result=present|absent|failed
```

异常保留并抛出，不伪装成 absent。

---

### RP-01 ImageAssetRepositoryImpl

状态：`🔧 待完成`

文件：

```text
imageasset/infrastructure/repository/ImageAssetRepositoryImpl.java
```

当前状态：

- Repository 功能已经存在；
- 无统一指标；
- MyBatis / MySQL 异常由框架直接抛出；
- 多个 update 方法用 boolean 表达成功、冲突或未更新，但语义尚未细分。

最终指标：

```text
IMAGE_ASSET_REPOSITORY_OPERATIONS
operation=save|find_by_id|find_by_ids|exists_by_hash|page_query|update_image_status|update_vector_status|increase_retry
result=success|not_found|conflict|duplicate|failed
```

```text
IMAGE_ASSET_REPOSITORY_DURATION
operation=...
result=...
```

记录位置：

- Repository 实现中包围真实数据库调用；
- 不记录业务日志；
- 数据库异常转换为 `SystemException` 时保留 cause；
- DuplicateKeyException 可继续交由上传用例转换成业务重复；
- 乐观锁 update=0 应表达 conflict，而不是笼统 failed。

---

## 9. Model Client 与 Qdrant

### MC-01 HttpModelVectorizatioinClient

状态：`🔧 待完成`

文件：

```text
modelclient/api/HttpModelVectorizatioinClient.java
```

当前问题：

- 类名存在 `Vectorizatioin` 拼写错误；
- 无高层 Client 指标；
- 模型非法响应使用 `BizException`，会被同步链路错误地记录为业务拒绝。

最终指标：

```text
MODEL_CLIENT_REQUESTS
operation=vectorize_image|vectorize_text|health
source=search|vectorization|health_check
result=success|timeout|unavailable|invalid_response|failed
```

```text
MODEL_CLIENT_DURATION
operation=...
source=...
result=...
```

异常分类：

- 调用方输入非法可以是 `BizException`；
- 模型服务超时、不可达、HTTP 错误、非法响应必须是 `SystemException`；
- Client 不记录后继续抛出的错误日志。

---

### MC-02 PythonModelHttpClientImpl

状态：`🔧 待完成`

当前问题：

- `ResourceAccessException`、`RestClientException` 被转换为 `BizException`；
- 无 HTTP 传输指标；
- 文本请求非法时错误码误用了 `IMAGE_INPUT_TYPE_INVALID`。

最终指标：

```text
MODEL_HTTP_REQUESTS
endpoint=vectorize_image|vectorize_text|health
result=success|timeout|unavailable|http_error|decode_error
```

```text
MODEL_HTTP_DURATION
endpoint=...
result=...
```

异常分类：

```text
timeout -> SystemException(MODEL_SERVICE_TIMEOUT)
connection/refused -> SystemException(MODEL_SERVICE_UNAVAILABLE)
HTTP/client error -> SystemException(MODEL_SERVICE_ERROR)
response decode -> SystemException(MODEL_RESPONSE_INVALID)
```

必须保留 cause，不记录后继续抛出的错误日志。

---

### MC-03 DefaultModelResponseValidator

状态：`🔧 待完成`

当前问题：

- 空响应、success=false、空向量、维度非法、模型名非法全部抛 `BizException`；
- 无非法响应指标；
- `VectorizeType.valueOf` 大小写约定与高层 Client 不完全一致。

最终指标：

```text
MODEL_RESPONSE_INVALID
operation=vectorize_image|vectorize_text|health
reason=empty_response|success_false|empty_vector|dimension_invalid|model_name_invalid|type_invalid
```

最终行为：

- 来自模型服务的非法响应统一转为 `SystemException`；
- 用户文本为空等调用前参数问题由调用入口校验，不混入模型响应错误；
- Validator 不记录日志。

---

### VI-01 QdrantVectorSearchClient.search

状态：`🔧 待完成`

文件：

```text
vectorindex/infrastructure/qdrant/QdrantVectorSearchClient.java
```

当前问题：

- 捕获所有异常后仅返回 `success=false`；
- cause 丢失；
- 无指标；
- 上层 Pipeline 再把失败结果转换成 `BizException`，最终成为业务拒绝。

最终指标：

```text
VECTOR_INDEX_REQUESTS
operation=search
result=success|rejected|failed
```

```text
VECTOR_INDEX_DURATION
operation=search
result=success|rejected|failed
```

```text
VECTOR_INDEX_SEARCH_RESULT_COUNT
```

最终行为：

- 请求参数非法按调用边界决定 `BizException` 或 `SystemException`；
- Qdrant 网络、超时、RPC 和序列化异常抛 `SystemException`；
- 保留 cause；
- Client 不记录后继续抛出的错误日志。

---

### VI-02 QdrantVectorIndexClient.upsert / delete / exists

状态：`🔧 待完成`

当前问题：

- 捕获异常并返回失败结果；
- cause 丢失；
- 无指标。

最终指标：

```text
VECTOR_INDEX_REQUESTS
operation=upsert|delete|exists
result=success|rejected|failed
```

```text
VECTOR_INDEX_DURATION
operation=upsert|delete|exists
result=success|rejected|failed
```

最终行为：

- Qdrant 基础设施异常抛 `SystemException`；
- 明确的业务失败结果只保留给确实不需要 Throwable 的场景；
- 不记录后继续抛出的错误日志。

---

### VC-01 DefaultActiveCollectionResolver

状态：`🔧 待完成`

文件：

```text
vectorindex/collection/DefaultActiveCollectionResolver.java
```

当前只直接返回配置。

最终指标：

```text
VECTOR_COLLECTION_RESOLVE_REQUESTS
result=success|failed
```

```text
VECTOR_COLLECTION_RESOLVE_DURATION
result=success|failed
```

记录位置可以在 Resolver 自身，因为该指标同时被搜索、向量化和健康检查复用。

正常解析不记日志；配置异常继续抛给最终链路收口点。

---

## 10. 搜索链路

### SE-01 DefaultSearchApplicationService.search

状态：`❌ 阻断`

文件：

```text
search/application/service/DefaultSearchApplicationService.java
```

当前问题：

- 仍写 `SEARCH_START`；
- 仍写 `SEARCH_SUCCESS`；
- catch 后写失败日志再继续抛出；
- 使用已删除的 LogService 方法；
- 无最终搜索指标。

最终职责：

```text
SEARCH_REQUESTS
search_type=image|text|hybrid
result=success|rejected|failed
cache_result=hit|miss|error|not_applicable
```

```text
SEARCH_DURATION
同上固定标签
```

```text
SEARCH_RESULT_COUNT
search_type=...
```

```text
SEARCH_EMPTY_RESULTS
search_type=...
```

日志：

- 不记录开始；
- 不记录成功；
- 不记录空结果；
- catch 只判断 result，随后继续抛出；
- 同步异常由 `GlobalExceptionHandler` 记录。

实现注意：

- 一次搜索只能增加一次 `SEARCH_REQUESTS`；
- 一次搜索只能记录一次 `SEARCH_DURATION`；
- 需要从 Pipeline 或 SearchContext 获得最终 cache_result。

---

### SE-02 DefaultSearchPipeline.execute

状态：`🔧 待完成`

当前已经具备完整流程：

```text
validate
resolve_collection
resolve_top_k
result_cache_get
query_vectorize
embedding_validate
overfetch
vector_recall
backfill
filter
rerank
assemble
result_cache_put
```

当前缺口：

- 无阶段指标；
- 无 recall/backfill/filter 数量指标；
- 内部系统失败被错误包装成 `BizException`。

阶段指标统一在 Pipeline 编排点记录，不向每个纯领域策略类注入 `MetricRecorder`。

最终阶段指标：

```text
SEARCH_STAGE_DURATION
search_type=...
stage=validate|resolve_collection|result_cache_get|query_vectorize|embedding_validate|overfetch|vector_recall|backfill|filter|rerank|assemble|result_cache_put
result=success|rejected|failed|skipped|degraded
```

数量指标：

```text
SEARCH_PIPELINE_ITEM_COUNT
search_type=...
stage=recall|backfill_requested|backfill_returned|filter_output
```

```text
SEARCH_FILTERED_ITEM_COUNT
search_type=...
reason=orphan_vector|invalid_image
```

```text
SEARCH_TOP_N
search_type=...
```

异常修正：

- 用户参数、topK 非法：`BizException`；
- 模型返回空 embedding：`SystemException`；
- embedding 维度与 collection 不一致：`SystemException`；
- Qdrant 失败：`SystemException`；
- Repository/MySQL 失败：`SystemException`。

---

### SE-03 搜索输入样本

状态：`🔧 待完成`

记录位置：

- 可在 `DefaultSearchApplicationService` 进入用例时记录；
- 也可在 Pipeline 完成校验后记录；
- 只能选一个入口，避免重复。

指标：

```text
SEARCH_QUERY_IMAGE_SIZE
search_type=image|hybrid
```

```text
SEARCH_QUERY_TEXT_LENGTH
search_type=text|hybrid
```

不得记录原始图片内容或 queryText。

---

### SE-04 QueryEmbedding 非法结果

状态：`🔧 待完成`

记录位置：

```text
DefaultSearchPipeline.validateQueryEmbedding
```

指标：

```text
SEARCH_EMBEDDING_INVALID
search_type=...
reason=empty|dimension_mismatch
```

异常：

- 这是模型、配置或内部一致性失败；
- 使用 `SystemException`；
- 不作为业务拒绝事件。

---

## 11. 任务发布、上传与异步向量化

### VP-01 ThreadPoolVectorizationTaskPublisher.publish

状态：`🔧 待完成`

文件：

```text
vectorization/infrastructure/publisher/ThreadPoolVectorizationTaskPublisher.java
```

当前问题：

- 无指标；
- 捕获异常后丢失 Throwable；
- 失败仅转换为结果；
- 上传 Application Service 越权记录失败。

最终指标：

```text
VECTORIZATION_PUBLISH_REQUESTS
source=image_upload|scanner|manual
result=success|invalid|rejected|failed
```

```text
VECTORIZATION_PUBLISH_DURATION
source=...
result=...
```

线程池拒绝：

```text
EXECUTOR_REJECTIONS
executor=vectorization
```

日志责任：

- 非法 command 且无 Throwable：`recordError(context)`；
- `RejectedExecutionException`：记录一次 `TASK_PUBLISH_FAILED`，保留 Throwable 时使用 `recordError`；
- 其他提交异常：`recordError(context, throwable)`；
- 提交成功不记日志；
- 调用方不得重复记录发布失败。

---

### EX-01 Executor Gauge Binder

状态：`🔧 待完成`

当前 `MetricName` 已定义 Gauge，但尚未绑定实际线程池对象。

需要增加独立 Binder，注册：

```text
EXECUTOR_ACTIVE
EXECUTOR_POOL_SIZE
EXECUTOR_MAX_POOL_SIZE
EXECUTOR_QUEUE_SIZE
EXECUTOR_QUEUE_REMAINING_CAPACITY
EXECUTOR_COMPLETED_TASKS
```

固定标签：

```text
executor=vectorization
```

Gauge 不通过 `MetricRecorder.recordValue` 反复写入。

---

### UP-01 DefaultImageAssetApplicationService.upload

状态：`❌ 阻断`

文件：

```text
imageasset/application/service/DefaultImageAssetApplicationService.java
```

当前问题：

- 使用旧 LogService 方法；
- 使用已删除的 `UPLOAD_COST_MS`；
- 记录上传开始和成功日志；
- 无最终用例指标；
- `readFileBytes` 转换 IOException 时丢失 cause；
- 发布失败由 Application Service 重复记录。

最终用例指标：

```text
IMAGE_UPLOAD_REQUESTS
result=success|rejected|failed
```

```text
IMAGE_UPLOAD_DURATION
result=success|rejected|failed
```

```text
IMAGE_UPLOAD_FILE_SIZE
```

阶段指标统一由该 Application Service 包围调用：

```text
IMAGE_UPLOAD_STAGE_DURATION
stage=read_bytes|validate|metadata_extract|hash|duplicate_check|storage_save|metadata_save|publish
result=success|rejected|failed|degraded
```

其他指标：

```text
IMAGE_UPLOAD_REJECTIONS
reason=<有限错误原因>
```

```text
IMAGE_UPLOAD_DUPLICATES
```

日志：

- 不记录开始和成功；
- 同步异常继续到 `GlobalExceptionHandler`；
- 发布失败由 Publisher 记录；
- 上传文件和元数据已成功时，即使任务发布失败，上传用例仍可记 success，publish 阶段记 degraded/failed。

---

### UP-02 compensateSavedFile

状态：`🔧 待完成`

当前问题：

- 删除异常被吞掉；
- Throwable 丢失；
- 只调用孤儿文件服务；
- 无补偿指标。

最终指标：

```text
IMAGE_UPLOAD_COMPENSATIONS
result=success|failed
reason=metadata_save_failed|duplicate_key
```

补偿删除失败：

1. 当前补偿边界记录一条 `recordError(context, throwable)`；
2. 随后由孤儿文件服务记录 `ORPHAN_FILE_DETECTED` 事件；
3. 增加 `ORPHAN_FILES`；
4. 不重复打印同一 Throwable。

---

### UP-03 LogOrphanFileLogService

状态：`❌ 阻断`

当前问题：

- 使用旧 `recordErrorLog`；
- 使用不存在的 `ORPHAN_FILE_RECORD`；
- 孤儿文件被当成普通错误而不是待处理事件。

最终事件：

```text
recordEvent
eventName=ORPHAN_FILE_DETECTED
status=WAITING
bizId=imageId
```

规则：

- 不在事件日志重复打印删除失败 Throwable；
- 不记录原始用户文件名、完整访问 URL 等非必要字段；
- 孤儿文件数量使用 `ORPHAN_FILES` 指标。

---

### VZ-01 DefaultImageVectorizationProcessor.process

状态：`🔧 待完成`

文件：

```text
vectorization/application/processor/DefaultImageVectorizationProcessor.java
```

当前问题：

- 无用例指标和阶段指标；
- 无唯一异步错误日志；
- `BizException` 和所有 Exception 最终大量映射为 `MODEL_SERVICE_ERROR`；
- 文件读取错误被错误包装为 `BizException`；
- READY 更新失败、Qdrant 已写入后的补偿语义未完整表达；
- asset not found、skipped 等结果无指标。

最终用例指标：

```text
VECTORIZATION_TASKS
source=image_upload|scanner|manual
result=success|failed|skipped|retrying
reason=<有限 FailReason>
```

```text
VECTORIZATION_DURATION
source=...
result=success|failed|skipped|retrying
```

阶段指标：

```text
VECTORIZATION_STAGE_DURATION
stage=asset_query|mark_processing|storage_check|storage_read|resolve_collection|model_inference|dimension_validate|vector_upsert|mark_ready
result=success|not_found|conflict|failed
```

专用指标：

```text
VECTORIZATION_DIMENSION_MISMATCH
source=image_vectorization
```

```text
VECTORIZATION_READY_UPDATE_FAILURES
```

日志责任：

- Processor 是异步链路唯一 Throwable 错误日志责任点；
- 捕获异常后先 `recordError`，再交给 FailureHandler 转换状态；
- FailureHandler 不重复记录 Throwable；
- 正常 skipped 不记日志。

异常分类：

- 文件、模型、Qdrant、MySQL 等基础设施错误均按系统错误；
- 不使用 BizException 表达内部模型或文件失败；
- FailReason 必须根据 errorCode/异常类型准确映射，不能全部归为 MODEL_SERVICE_ERROR。

---

### VZ-02 DefaultVectorizationFailureHandler

状态：`🔧 待完成`

文件：

```text
vectorization/domain/failure/DefaultVectorizationFailureHandler.java
```

当前已经完成状态更新，但没有事件记录。

最终事件：

文件缺失：

```text
IMAGE_FILE_MISSING
status=INVALIDATED
```

可重试：

```text
VECTORIZATION_RETRY_SCHEDULED
status=RETRYING
```

最终失败：

```text
VECTORIZATION_DEAD_FAILED
status=FAILED
```

READY 补偿：

```text
VECTOR_READY_COMPENSATION_REQUIRED
status=WAITING
```

指标：

```text
VECTORIZATION_RETRIES
reason=...
```

同时任务最终结果仍由 Processor 统一记录到 `VECTORIZATION_TASKS`，避免 Handler 和 Processor 双计数。

---

## 12. 图片访问

### IA-01 ImageFileController.getImage

状态：`🔧 待完成`

文件：

```text
imageasset/interfaces/controller/ImageFileController.java
```

当前已经可以读取并返回 `StreamingResponseBody`，但完全没有观测能力。

同步入口指标：

```text
IMAGE_ACCESS_REQUESTS
result=success|not_found|invalid_path|failed
```

```text
IMAGE_ACCESS_DURATION
result=success|not_found|invalid_path|failed
```

```text
IMAGE_ACCESS_RESPONSE_SIZE
```

同步异常继续抛给 `GlobalExceptionHandler`。

---

### IA-02 StreamingResponseBody 写出

状态：`🔧 待完成`

当前 lambda 只执行 `transferTo`，无计时、字节数和错误分类。

指标：

```text
IMAGE_ACCESS_STREAM_DURATION
result=success|client_aborted|failed
```

```text
IMAGE_ACCESS_TRANSFERRED_BYTES
```

日志：

- 客户端主动断开通常只记 `client_aborted` 指标，不打 ERROR；
- 服务器端流写出失败由 lambda 自己记录 `recordError`；
- lambda 执行时不得假设原 Servlet 线程 MDC 仍然存在，需要提前捕获日志上下文。

注意：

`IMAGE_ACCESS_REQUESTS` 和 `IMAGE_ACCESS_DURATION` 应定义为 Controller 同步准备阶段，还是包含异步流式完成阶段，实施时必须固定口径。推荐：

- Controller 指标表示资源解析与响应建立；
- Stream 指标表示实际数据写出。

---

## 13. 健康检查

### HC-01 SystemHealthCheckService.check

状态：`🔧 待完成`

文件：

```text
system/health/SystemHealthCheckService.java
```

当前已经完成 MySQL、Qdrant、Model Service 检查和 latestSnapshot 保存，但：

- 无整体指标；
- 无组件指标；
- 无 Gauge；
- 无 UP/DOWN 状态迁移识别；
- 异常 message 直接进入健康结果；
- 无状态变化事件。

整体指标：

```text
HEALTH_CHECKS
source=startup|scheduled
result=up|down|failed
```

```text
HEALTH_DURATION
source=startup|scheduled
result=up|down|failed
```

组件指标：

```text
HEALTH_COMPONENT_CHECKS
component=mysql|qdrant|model_service
result=up|down|failed
```

```text
HEALTH_COMPONENT_DURATION
component=mysql|qdrant|model_service
result=up|down|failed
```

Gauge：

```text
HEALTH_COMPONENT_STATUS
component=mysql|qdrant|model_service
1=UP
0=DOWN
```

事件：

```text
UP -> DOWN: SYSTEM_COMPONENT_DOWN
DOWN -> UP: SYSTEM_COMPONENT_RECOVERED
```

持续 DOWN 不重复刷日志。

---

### HC-02 StartupHealthCheckHook

状态：`🔧 待完成`

当前 fail-fast 已实现，但未记录启动失败事件。

要求：

- 调用健康检查时传递 `source=startup`；
- fail-fast 前记录 `STARTUP_HEALTH_CHECK_FAILED`；
- 有 Throwable 时记录错误堆栈；
- 不记录启动健康成功日志。

---

### HC-03 ScheduledHealthCheckHook

状态：`🔧 待完成`

要求：

- 调用健康检查时传递 `source=scheduled`；
- 不记录每次定时开始或成功；
- 状态变化由 `SystemHealthCheckService` 统一处理。

---

## 14. Python modelservice

本轮仍不处理：

```text
模型加载
tokenizer 加载
warm-up
startup
shutdown
```

### PY-01 `POST /vectorize/image`

状态：`🔧 待完成`

文件：

```text
modelservice/app/api.py
```

当前问题：

- 请求进入时写 info 日志；
- 无接口 Counter 和 Timer；
- inference lock 等待时间未记录；
- 文件大小未记录。

最终指标：

```text
everypicfound_modelservice_requests_total
endpoint=vectorize_image
result=success|rejected|failed
```

```text
everypicfound_modelservice_request_duration_seconds
endpoint=vectorize_image
result=success|rejected|failed
```

```text
everypicfound_modelservice_inference_lock_wait_seconds
vectorize_type=image
```

```text
everypicfound_modelservice_image_size_bytes
```

删除正常请求进入日志。

---

### PY-02 `POST /vectorize/text`

状态：`🔧 待完成`

最终指标：

```text
everypicfound_modelservice_requests_total
endpoint=vectorize_text
result=success|rejected|failed
```

```text
everypicfound_modelservice_request_duration_seconds
endpoint=vectorize_text
result=success|rejected|failed
```

```text
everypicfound_modelservice_inference_lock_wait_seconds
vectorize_type=text
```

```text
everypicfound_modelservice_text_length
```

删除正常请求进入日志，不记录原始 text。

---

### PY-03 VectorizationService 推理

状态：`🔧 待完成`

文件：

```text
modelservice/app/vectorization_service.py
```

当前问题：

- 图片和文本推理成功均写 info 日志；
- `_fail` 使用 warning，但没有指标；
- 推理耗时仅作为响应字段和日志字段，没有 Prometheus 指标；
- 某些明确失败没有 traceback，未知异常有 traceback，方向基本正确。

最终指标：

```text
everypicfound_modelservice_inference_duration_seconds
vectorize_type=image|text
result=success|failed
```

日志：

- 删除 `VECTORIZE_IMAGE_SUCCESS`；
- 删除 `VECTORIZE_TEXT_SUCCESS`；
- 明确输入/维度失败可以记录结构化失败但不伪造 traceback；
- 未知推理异常保留 `logger.exception` 和 traceback；
- 不重复由 API 层再次记录相同推理错误。

---

### PY-04 `/health`

状态：`🔧 待完成`

当前问题：

- 每次健康检查都写 info；
- API 和 Service 都可能记录失败；
- 无 Counter、Timer、Gauge；
- 无状态迁移控制。

最终指标：

```text
everypicfound_modelservice_health_checks_total
result=up|down|failed
```

```text
everypicfound_modelservice_health_check_duration_seconds
result=up|down|failed
```

```text
everypicfound_modelservice_health
1=UP
0=DOWN
```

日志：

- 正常 UP 不记日志；
- UP -> DOWN 记录一次；
- DOWN -> UP 记录恢复事件；
- 持续 DOWN 不重复刷日志；
- API 层与 Service 层只能选择一个最终错误收口点。

---

## 15. 当前实施顺序

### 第一批：恢复公共接口一致性

```text
CFG-01
C-01
C-02
CR-01 ~ CR-04
CW-01 ~ CW-04
```

完成目标：

- Redis 不再吞基础设施异常；
- Cache Wrapper 能明确降级；
- 所有旧 Cache 日志接口引用消失；
- Prometheus 可以正常抓取。

### 第二批：Storage、Repository、Model、Qdrant

```text
ST-01 ~ ST-04
RP-01
MC-01 ~ MC-03
VI-01 ~ VI-02
VC-01
```

完成目标：

- 基础设施层统一指标；
- 基础设施异常统一为 SystemException；
- Adapter 不重复写错误日志；
- cause 链不丢失。

### 第三批：搜索

```text
SE-01 ~ SE-04
```

完成目标：

- 删除搜索开始、成功和重复失败日志；
- 搜索用例指标与 Pipeline 阶段指标完整；
- 系统失败不再伪装成 BizException；
- Cache hit/miss/error 可传递到用例指标。

### 第四批：任务发布、上传与异步向量化

```text
VP-01
EX-01
UP-01 ~ UP-03
VZ-01 ~ VZ-02
```

完成目标：

- Publisher 成为发布失败唯一责任点；
- 上传用例与阶段指标完整；
- 补偿失败和孤儿文件职责分离；
- Processor 成为异步 Throwable 唯一责任点；
- FailureHandler 只记录状态事件。

### 第五批：图片访问、健康检查和 Python

```text
IA-01 ~ IA-02
HC-01 ~ HC-03
PY-01 ~ PY-04
```

---

## 16. 每个入口的完成标准

完成一个编号入口时，必须同时满足：

1. 不再引用旧 `LogService` 方法；
2. 不再引用已经删除的 `MetricName` 或 `LogEventName`；
3. 指标使用当前 `MetricName`；
4. 标签只使用当前 `MetricTag`；
5. 同一个 MetricName 的标签键集合保持固定；
6. 同一异常只在最终责任点记录一次；
7. 普通成功不写业务日志；
8. SystemException 保留原始 cause；
9. 不将 requestId、traceId、imageId、文本、路径和原始 key 写入指标标签；
10. 增加对应单元测试或集成测试。

---

## 17. 每批完成后的扫描

Java 旧日志接口：

```bash
grep -R "recordBizLog\|recordSuccessLog\|recordErrorLog\|recordStateChangeLog\|recordSlowLog" \
  everypicfound-backend/src/main/java
```

裸业务日志：

```bash
grep -R "log\.info\|log\.warn\|log\.error" \
  everypicfound-backend/src/main/java/com/everypicfound
```

旧指标枚举：

```bash
grep -R "UPLOAD_COST_MS\|SEARCH_COST_MS\|FILE_SAVE_DURATION_MS\|FILE_READ_DURATION_MS\|FILE_DELETE_DURATION_MS\|FILE_.*FAILED_COUNT\|CACHE_ACCESS_TOTAL_COUNT" \
  everypicfound-backend/src/main/java
```

旧事件枚举：

```bash
grep -R "FILE_SAVE_SUCCESS\|FILE_SAVE_FAILED\|FILE_READ_SUCCESS\|FILE_READ_FAILED\|FILE_DELETE_SUCCESS\|FILE_DELETE_FAILED\|CACHE_GET_FAILED\|CACHE_PUT_FAILED\|CACHE_EVICT_FAILED\|CACHE_EXISTS_FAILED\|ORPHAN_FILE_RECORD" \
  everypicfound-backend/src/main/java
```

说明：

- 扫描结果不一定必须绝对为空；
- fallback Logger、框架启动日志等允许保留；
- 每一个结果必须人工确认不属于业务成功日志或重复错误日志。

---

## 18. 当前进度结论

截至 `mxl@1f0f261`：

### 已经完成

- 统一 LogService 最终接口；
- GlobalExceptionHandler 同步异常唯一收口；
- MetricRecorder 和 Micrometer 实现；
- 最终 MetricName；
- 最终 MetricTag；
- Gauge 类型定义；
- 最终 LogEventName；
- HTTP RequestContext 主体能力；
- 异步 RequestContext/MDC 传播主体能力；
- Prometheus Docker 服务骨架。

### 当前真实阻断

- Prometheus `metrics_path` 拼写错误；
- 多个业务类仍引用已删除 LogService 方法；
- 多个业务类仍引用已删除 MetricName；
- 多个业务类仍引用已删除 LogEventName；
- Redis、Storage、Model、Qdrant 的异常契约仍未统一；
- 搜索和上传仍保留开始/成功/重复失败日志。

### 尚未接入

- Cache/Redis 最终指标和降级事件；
- Storage/Repository 最终指标；
- Model/Qdrant 最终指标；
- 搜索用例与 Pipeline 指标；
- 上传用例与阶段指标；
- Publisher 和异步向量化指标/事件；
- Image Access 指标；
- Executor/Health Gauge Binder；
- Python Prometheus 指标。

下一施工入口：

```text
CFG-01 → CR-01
```

先修复 Prometheus 抓取路径，再从 `RedisCacheService.get` 开始迁移。
