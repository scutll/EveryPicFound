# EveryPicFound

## 项目概述

EveryPicFound 是一个多模态搜图项目，核心链路是：

`图片上传/管理 -> 图片向量化 -> 向量入库 -> 图搜图 / 文搜图 / 图文混搜`

仓库当前包含：

- `identity-service`：用户、认证、Token 与 Session 等身份能力。
- `gateway-service`：统一入口、路由、认证状态校验与授权。
- `media-search-service`：图片资产、向量化和搜索主业务。
- `security-contract`：跨服务共享的最小安全契约。
- `modelservice`：Python/FastAPI 模型服务，提供图片/文本向量化。
- `everypicfound-frontend`：前端展示与搜索页面。
- `docs`：项目设计与开发文档，优先阅读。

## 后端服务与模块结构

`media-search-service` 采用按业务模块拆分、模块内部分层的结构，主模块如下：

- `imageasset`：图片上传、元数据、去重、状态管理、图片访问。
- `vectorization`：异步向量化任务发布、处理、失败重试、查询向量化与图文融合。
- `search`：搜索入口与统一 `SearchPipeline`，负责图搜图、文搜图、图文混搜。
- `storage`：文件存储适配，当前以本地存储为主。
- `modelclient`：Java 调用 Python 模型服务的适配层。
- `vectorindex`：Qdrant 向量库适配、collection 配置、向量写入与召回。
- `common`：异常、日志、指标、上下文、线程池、缓存、限流等公共能力。
- `system`：系统健康检查与可观测性补充能力。

常见分层约定：

- `interfaces`：Controller / Request / Response
- `application`：用例编排、pipeline、processor、command、dto
- `domain`：规则、策略、状态、模型
- `infrastructure`：MySQL、Qdrant、Redis、HTTP、本地文件等适配

## 建议优先阅读

先读 `docs/README.md` 了解文档分类、权威来源和维护规则，再看相关文档与代码。建议顺序：

1. `docs/project/EveryPicFound_PRD.md`
2. `docs/project/系统架构设计文档.md`
3. `docs/project/模块设计文档.md`
4. `docs/project/数据模型设计文档.md`
5. `docs/project/interface_docs.md`
6. `docs/modules/第一轮任务文档 - 图片上传链路.md`
7. `docs/modules/第二轮任务文档-图片向量化与向量入库链路.md`
8. `docs/modules/Python模型服务架构设计文档.md`

如果是直接进入后端开发，优先看这些代码入口：

- `identity-service/src/main/java/com/everypicfound/identity/IdentityServiceApplication.java`
- `identity-service/src/main/resources/application.yaml`
- `gateway-service/src/main/java/com/everypicfound/gateway/GatewayApplication.java`
- `gateway-service/src/main/resources/application.yaml`
- `media-search-service/src/main/java/com/everypicfound/MediaSearchServiceApplication.java`
- `media-search-service/src/main/resources/application.yaml`

## 文档记录与维护

- 开始任务前先阅读 `docs/README.md`，再读取与当前业务模块、技术和测试相关的索引及权威文档。
- 遇到技术选型、参数选择、接口或数据变化、缓存或消息契约、事务和并发规则、编码规范、工具用法、测试结果、故障原因或重要修正时，检查是否需要记录或更新文档。
- 写文档前先与用户讨论记录范围，确认是更新现有文档还是新增文档，以及内容应归入 `project`、`modules`、`technologies` 或 `reports`。
- `modules` 记录 EveryPicFound 中采用的具体方案和理由；`technologies` 记录可脱离项目复用的通用原理与用法。完整内容只保留一份，其他位置使用摘要和链接。
- 代码或配置变化导致旧文档失效时，必须在当前任务内同步修正文档；暂未迁移或文件较旧不能成为保留错误内容的理由。
- 编码切片结束时记录关键决策、验证证据、使用命令、失败原因和遗留问题；出现长期复用价值时，再提炼为通用技术分析。
- 文档中不得记录密码、Token、Cookie、真实密钥或其他敏感内容；修改或移动文档后检查所有本地链接。
- `docs/` 只保存用户确认需要长期维护的项目资料。不得把 Codex 自用任务计划、临时 spec、推理草稿或中间检查结果写入 `docs/`；用户明确要求的项目计划不受此限制。

## 性能测试纪律

- 压测任务必须严格串行执行。
- 同一时间只允许运行一个压测脚本程序。
- 必须等上一项测试完整结束并完成结果落盘后，才能开始下一项测试。
- 不允许为了节省时间并行运行多个 `k6` 脚本，也不允许把多个独立测试场景混在同一时间窗内执行。
- 如果发现执行方式可能污染测试结果，当前批次结果必须作废，并明确标记为无效后重新测试。

## 反思记录

- 之前的压测执行里出现过并行或半并行的错误方式，导致不同场景/批次之间相互污染结果。
- 后续所有性能测试一律采用“单脚本、单场景、单时间窗、单批次串行”原则，优先保证结果可信，再考虑效率。
- Windows 环境下不要在压测前临时拼接 `Start-Process`、`cmd /c`、`powershell -Command` 这类后台启动命令，尤其不要一边改引号转义一边试跑。启动命令必须先用前台方式验证可用，再沉淀为固定脚本。
- 健康检查只能用于确认服务是否已经就绪，不能代替启动命令本身的正确性验证。若启动失败，必须先查看前台报错或启动日志，区分是服务代码问题、环境问题，还是 PowerShell/Maven/conda 参数转义问题。
- 后续涉及 `modelservice` 和 Spring 后端的重启时，应优先提供稳定的脚本化入口，至少固定启动命令、日志落盘、PID 记录和 30 秒健康检查，避免临场手工拼命令造成误判。
