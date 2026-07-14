# EveryPicFound 性能测试报告

## 1. 测试目标

本轮性能测试目标是建立 EveryPicFound 搜索链路的可复现性能基线，并评估 Redis 文本向量缓存与搜索结果缓存对文本搜索和完整页面链路的收益。

测试重点包括：

- 纯文本搜索接口吞吐和延迟
- 文本搜索后并发加载全部返回图片的完整页面链路
- 图片搜索、混合搜索的早期 baseline 参考结果
- Redis 缓存关闭、缓存开启但未命中、缓存热点命中三类文本场景
- `query_vectorize` 不再是瓶颈后，高并发下图片访问链路的饱和情况
- K6 客户端指标与后端 Prometheus 指标的阶段拆分对照

## 2. 测试环境与口径

后端 profile：

- baseline：`dev,perf-baseline`
- cache：缓存开启配置，实际 miss/hit 通过 K6 查询策略区分

核心配置：

- `TOP_K=30`
- 常规矩阵：`VU=10/15/20/25/30`
- 高并发页面矩阵：`VU=60/80/100`
- 每轮常规矩阵持续 `40s`

主要脚本：

- `performance-test/scripts/text-search-baseline.js`
- `performance-test/scripts/text-search-image-access-baseline.js`
- `performance-test/scripts/image-search-baseline.js`
- `performance-test/scripts/image-search-image-access-baseline.js`
- `performance-test/scripts/hybrid-search-baseline.js`
- `performance-test/scripts/hybrid-search-image-access-baseline.js`

结果口径：

- 搜索接口使用 `qps / avg / p95 / p99`
- 完整页面链路使用 `page/s / page_total_duration / page_search_duration / image_access_duration`
- 服务端阶段耗时来自每轮测试前后的 `/actuator/prometheus` 快照增量
- 本报告中的缓存 miss/hit 结果只采用串行重跑后的 `*-final` 数据

## 3. 数据有效性说明

早期缓存 miss 测试曾出现异常高 QPS。复核后确认根因是旧版 `unique` 查询只包含基础文本、`vu`、`iter`，在连续执行 VU 矩阵时发生 key 复用，导致 miss 场景混入 hit。

修正方式：

- `unique` 查询加入 `RUN_ID`
- miss/hit 场景全部串行重跑
- 压测纪律写入 `AGENTS.md`：同一时间只允许运行一个压测脚本，上一项完整结束并落盘后再开始下一项

本报告采用的数据：

- 可用：`text-baseline-prom-20260701-180609`
- 可用：`text-cache-miss-prom-20260701-final`
- 可用：`text-cache-hit-prom-20260701-final`
- 可用：`text-cache-hit-highvu-page-20260701-serial`
- 仅作历史参考：早期 `summary-all-profiles-40s-topk30.csv` 中的 baseline 部分
- 不纳入结论：早期被污染的 text/hybrid cache-miss 批次

## 4. 阶段一：初始低并发基线

### 4.1 纯文本搜索 baseline

条件：

- `10 VU`
- `3 分钟`
- 缓存关闭

结果：

| 指标 | 数值 |
|---|---:|
| 搜索请求数 | 19612 |
| QPS | 108.93/s |
| Avg | 91.55 ms |
| P95 | 103.84 ms |
| P99 | 122.65 ms |
| Max | 217.95 ms |
| HTTP 失败率 | 0% |
| 业务失败率 | 0% |

这轮用于快速确认文本搜索基线能力，后续 40s 矩阵重测的 QPS 略低，但结论一致：文本搜索在较低并发下很快进入吞吐平台。

### 4.2 文本搜索 + 全部图片加载 baseline

条件：

- `10 VU`
- `30 秒`
- `TOP_K=30`
- 每次搜索后并发加载全部返回图片

结果：

| 指标 | 数值 |
|---|---:|
| 完整页面迭代数 | 2058 |
| 页面吞吐 | 68.18 page/s |
| 搜索请求数 | 2058 |
| 图片请求数 | 61740 |
| 总 HTTP QPS | 2113.65/s |
| 搜索 Avg / P95 / P99 | 106.34 / 140.45 / 193.95 ms |
| 单图访问 Avg / P95 / P99 | 6.95 / 12.02 / 19.81 ms |
| 图片批次 Avg / P95 / P99 | 38.74 / 49.00 / 68.86 ms |
| 页面 Avg / P95 / P99 | 146.39 / 190.00 / 232.43 ms |
| 失败率 | 0% |

这轮说明在低并发、本机回环和本地文件缓存条件下，图片访问本身很快，单图平均约 `7 ms`。

## 5. 阶段二：多接口 baseline 矩阵

早期 baseline 矩阵覆盖了文本、图片、混合搜索及其完整页面链路。该矩阵可用于横向比较各接口基础吞吐，不用于缓存收益结论。

代表结果：

| 场景 | VU | 吞吐 | Avg ms | P95 ms |
|---|---:|---:|---:|---:|
| text-search | 10 | 98.98 qps | 100.61 | 121.40 |
| text-search | 30 | 100.45 qps | 297.38 | 898.74 |
| image-search | 10 | 65.04 qps | 153.25 | 172.79 |
| image-search | 30 | 65.12 qps | 458.32 | 1375.10 |
| hybrid-search | 10 | 36.72 qps | 271.65 | 295.51 |
| hybrid-search | 30 | 36.76 qps | 810.38 | 1463.74 |

完整页面链路代表结果：

| 场景 | VU | 页面吞吐 | Page Avg ms | Page P95 ms |
|---|---:|---:|---:|---:|
| text-search + images | 10 | 61.78 page/s | 161.68 | 207.00 |
| text-search + images | 30 | 60.04 page/s | 497.12 | 1201.80 |
| image-search + images | 10 | 50.70 page/s | 196.98 | 221.35 |
| image-search + images | 30 | 48.89 page/s | 609.43 | 1764.75 |
| hybrid-search + images | 10 | 32.34 page/s | 308.67 | 352.00 |
| hybrid-search + images | 30 | 32.53 page/s | 915.20 | 1657.85 |

结论：

- 各搜索类型在 `VU=10` 附近已经接近吞吐平台
- 继续增加到 `VU=30`，吞吐提升不明显，P95/P99 快速升高
- 混合搜索最重，图片搜索次之，文本搜索最轻

## 6. 阶段三：文本 baseline 与服务端阶段拆分

数据来源：`performance-test/results/text-baseline-prom-20260701-180609/summary-with-metrics.csv`

### 6.1 纯文本搜索

| VU | QPS | Avg ms | P95 ms | Server Avg ms | query_vectorize ms | Qdrant ms | MySQL ms |
|---|---:|---:|---:|---:|---:|---:|---:|
| 10 | 88.34 | 112.72 | 129.74 | 110.68 | 98.18 | 3.41 | 8.44 |
| 15 | 90.74 | 164.69 | 192.33 | 163.03 | 151.48 | 3.06 | 8.01 |
| 20 | 89.22 | 223.29 | 351.96 | 221.66 | 210.21 | 2.89 | 7.98 |
| 25 | 87.96 | 282.90 | 670.66 | 281.18 | 269.68 | 2.86 | 8.05 |
| 30 | 88.25 | 338.18 | 1021.24 | 336.48 | 324.97 | 2.93 | 8.01 |

结论：

- baseline 纯文本搜索约在 `VU=10` 达到平台，平台吞吐约 `88-91 qps`
- `query_vectorize` 占据绝大部分耗时
- Qdrant 召回稳定在 `2.8-3.4 ms`
- MySQL 回表稳定在 `6-8 ms`

### 6.2 文本搜索 + 全部图片加载

| VU | Page/s | Search Avg ms | Page Avg ms | Page P95 ms | K6 Image Avg ms | Server Image Avg ms |
|---|---:|---:|---:|---:|---:|---:|
| 10 | 58.79 | 125.72 | 169.83 | 204.00 | 7.47 | 4.77 |
| 15 | 58.85 | 209.29 | 254.24 | 296.00 | 7.65 | 5.09 |
| 20 | 55.23 | 311.71 | 361.08 | 520.00 | 8.21 | 5.61 |
| 25 | 59.30 | 364.27 | 419.70 | 922.00 | 9.30 | 6.24 |
| 30 | 59.65 | 453.18 | 500.39 | 1492.00 | 7.82 | 5.36 |

结论：

- baseline 页面链路平台约 `55-60 page/s`
- 页面耗时主要仍由搜索阶段里的 `query_vectorize` 决定
- 图片访问在 baseline 低并发阶段不是主瓶颈，单图服务端均值约 `4.8-6.2 ms`

## 7. 阶段四：缓存 miss/hit 对比

数据来源：

- `performance-test/results/text-cache-miss-prom-20260701-final/summary-with-metrics.csv`
- `performance-test/results/text-cache-hit-prom-20260701-final/summary-with-metrics.csv`

### 7.1 纯文本搜索

| Profile | VU | QPS | Avg ms | P95 ms | Server Avg ms | query_vectorize ms |
|---|---:|---:|---:|---:|---:|---:|
| baseline | 10 | 88.34 | 112.72 | 129.74 | 110.68 | 98.18 |
| cache-miss | 10 | 103.54 | 96.18 | 107.42 | 94.61 | 83.64 |
| cache-hit | 10 | 3873.08 | 2.27 | 3.22 | 0.98 | 0.00 |
| baseline | 30 | 88.25 | 338.18 | 1021.24 | 336.48 | 324.97 |
| cache-miss | 30 | 104.93 | 284.40 | 820.00 | 282.77 | 271.15 |
| cache-hit | 30 | 4446.76 | 6.31 | 8.59 | 4.73 | 0.00 |

结论：

- `cache-hit` 将纯文本搜索从百级 QPS 提升到四千级 QPS
- 命中缓存后，`query_vectorize` 基本归零
- `cache-miss` 没有出现明显额外负担，本轮结果略优于 baseline
- 真正的量级收益来自热点复用

### 7.2 文本搜索 + 全部图片加载

| Profile | VU | Page/s | Search Avg ms | Page Avg ms | Page P95 ms | K6 Image Avg ms | Server Image Avg ms |
|---|---:|---:|---:|---:|---:|---:|---:|
| baseline | 10 | 58.79 | 125.72 | 169.83 | 204.00 | 7.47 | 4.77 |
| cache-miss | 10 | 68.92 | 102.08 | 144.94 | 183.00 | 7.30 | 5.69 |
| cache-hit | 10 | 121.56 | 2.88 | 82.20 | 115.00 | 14.25 | 13.08 |
| baseline | 30 | 59.65 | 453.18 | 500.39 | 1492.00 | 7.82 | 5.36 |
| cache-miss | 30 | 63.67 | 414.22 | 468.60 | 1119.40 | 9.18 | 7.11 |
| cache-hit | 30 | 124.58 | 2.77 | 240.67 | 278.00 | 45.53 | 44.37 |

结论：

- `cache-hit` 下页面吞吐约提升到 baseline 的 2 倍
- 搜索阶段从数百毫秒降到约 `3 ms`
- 页面剩余耗时主要转移到图片批量访问
- `VU=30` 时 cache-hit 单图服务端均值已升至 `44.37 ms`，说明图片访问压力开始抬头

### 7.3 缓存标签验证

cache-miss：

- `search_cache_hit = 0`
- `search_cache_miss = server_search_count`
- `search_result_cache_get_miss = server_search_count`
- `text_vector_cache_get_miss = server_search_count`

cache-hit：

- 绝大多数请求为 `hit`
- 少量 miss 来自首次热身或首次访问
- 例如 `text-search-baseline VU=15`：`search_cache_hit = 176226`，`search_cache_miss = 0`

## 8. 阶段五：cache-hit 高并发页面链路

目标是在 `query_vectorize` 不再是主瓶颈后，继续增加 VU，观察图片访问和数据库阶段是否成为新瓶颈。

数据来源：`performance-test/results/text-cache-hit-highvu-page-20260701-serial/report.md`

场景：

- `text-search-image-access-baseline`
- `QUERY_MODE=fixed-pool`
- `TOP_K=30`
- `VU=60/80/100`

结果：

| VU | Page/s | Search Avg ms | Page Avg ms | Page P95 ms | K6 Image Avg ms | Server Image Avg ms | Qdrant Avg ms | MySQL Avg ms |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| 60 | 116.28 | 5.31 | 515.20 | 620.00 | 99.19 | 97.65 | 29.05 | 58.50 |
| 80 | 107.35 | 8.12 | 743.64 | 895.70 | 141.62 | 137.53 | 55.50 | 45.73 |
| 100 | 100.12 | 6.65 | 993.22 | 1197.00 | 192.62 | 189.42 | 8.00 | 21.90 |

结论：

- `VU=60` 后页面链路已经进入饱和区
- `VU=60 -> 80 -> 100` 时，页面吞吐从 `116.28` 降至 `100.12 page/s`
- 页面均值从 `515.20 ms` 升至 `993.22 ms`
- 搜索阶段仍只有 `5-8 ms`，不再是主瓶颈
- 单图服务端访问从 `97.65 ms` 升至 `189.42 ms`
- 新瓶颈已经转移到图片访问链路

## 9. 整体结论

### 9.1 饱和点

| 场景 | 饱和判断 |
|---|---|
| baseline 纯文本搜索 | `VU=10` 左右进入平台，约 `88-91 qps` |
| baseline 文本页面链路 | `VU=10` 左右进入平台，约 `55-60 page/s` |
| cache-miss 纯文本搜索 | `VU=10` 后稳定在约 `104-105 qps` |
| cache-miss 文本页面链路 | `VU=10` 后约 `64-69 page/s` |
| cache-hit 纯文本搜索 | `VU=30` 仍有 `3.8k-4.6k qps` |
| cache-hit 文本页面链路 | 常规 VU 下约 `115-133 page/s` |
| cache-hit 高并发页面链路 | `VU=60` 后进入退化区 |

### 9.2 主要瓶颈演进

baseline：

- 主瓶颈是文本向量化 `query_vectorize`
- Qdrant 和 MySQL 不是主要耗时来源
- 图片访问在低并发时很轻

cache-miss：

- 仍然需要执行 `query_vectorize`
- 链路形态接近 baseline
- 本轮 miss 结果略优于 baseline，但不具备量级收益

cache-hit：

- 搜索结果缓存和文本向量缓存命中后，`query_vectorize` 基本消失
- 纯文本搜索变为毫秒级
- 页面链路瓶颈从搜索转移到图片批量访问

高并发 cache-hit：

- 搜索阶段保持很轻
- 图片访问耗时随 VU 单调上升
- 页面吞吐下降，尾延迟显著恶化

### 9.3 缓存收益

缓存对文本搜索是有效的：

- 纯文本搜索从约 `88 qps` 提升到约 `4k qps`
- 搜索阶段从百毫秒级降到毫秒级
- 完整页面链路吞吐约提升到 baseline 的 2 倍

缓存对图片访问本身没有优化作用：

- 搜索阶段被压缩后，页面链路会暴露图片访问瓶颈
- 高 VU 下图片访问从低并发的 `5-45 ms` 上升到 `100-190 ms`

## 10. 后续建议

1. 单独压测 `/images/**` 图片访问链路，去掉搜索影响，确认文件读取、响应流式传输和 Tomcat 线程占用情况。
2. 为图片访问阶段补充更细粒度指标，例如元数据查询耗时、文件存在性检查耗时、流式写出耗时、响应大小分布。
3. 对 cache-hit 页面链路继续观察 JVM、Tomcat 线程、磁盘 IO、网络发送速率，判断图片访问退化是线程、IO 还是响应体传输造成。
4. 如果目标是提升页面吞吐，下一步优化优先级应从 `query_vectorize` 转向图片访问链路。
5. 保持压测严格串行执行，任何并行或时间窗重叠的结果都不应作为正式结论。

## 11. 参考文件

测试脚本：

- `performance-test/scripts/text-search-baseline.js`
- `performance-test/scripts/text-search-image-access-baseline.js`
- `performance-test/scripts/run-text-baseline-with-metrics.ps1`
- `performance-test/scripts/summarize-text-baseline-with-metrics.ps1`

结果目录：

- `performance-test/results/text-baseline-prom-20260701-180609`
- `performance-test/results/text-cache-miss-prom-20260701-final`
- `performance-test/results/text-cache-hit-prom-20260701-final`
- `performance-test/results/text-cache-hit-highvu-page-20260701-serial`
- `performance-test/results/summary-all-profiles-40s-topk30.csv`

关键后端代码：

- `everypicfound-backend/src/main/java/com/everypicfound/search/application/pipeline/DefaultSearchPipeline.java`
- `everypicfound-backend/src/main/java/com/everypicfound/search/application/service/DefaultSearchApplicationService.java`
- `everypicfound-backend/src/main/java/com/everypicfound/imageasset/interfaces/controller/ImageFileController.java`
- `everypicfound-backend/src/main/java/com/everypicfound/imageasset/infrastructure/repository/ImageAssetRepositoryImpl.java`
- `everypicfound-backend/src/main/java/com/everypicfound/vectorindex/infrastructure/qdrant/QdrantVectorSearchClient.java`
- `everypicfound-backend/src/main/java/com/everypicfound/common/metric/MetricName.java`
