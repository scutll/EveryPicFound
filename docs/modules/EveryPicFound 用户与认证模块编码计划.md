# EveryPicFound 用户与认证模块编码计划

## 一、计划目标

本计划用于指导 `identity-service`、`gateway-service` 和 `media-search-service` 完成用户与认证功能。

当前开发原则是：

> 先完成可运行、可测试、可演示的完整业务闭环，再根据已经出现的问题逐项增加 Redis、MQ、Outbox、Lua、缓存和分布式一致性机制。

计划分成两部分：

1. **功能主线**：注册、登录、受保护访问、Session、刷新、退出和用户信息。
2. **优化支线**：登录保护、即时撤销、缓存、消息可靠性、密钥轮换和性能优化。

功能主线中的每个阶段都必须能够独立运行和验收。优化支线不能反向成为基础功能的前置条件。

本计划是当前实施顺序和阶段边界的权威来源。以下文档继续保存字段、接口和目标架构的详细设计，但其中的未来方案不等于当前必须实现：

- [EveryPicFound 用户与认证模块设计](EveryPicFound_用户与认证模块设计.md)
- [基于 JWT 的登录认证与会话管理技术设计](基于JWT的登录认证与会话管理技术设计文档.md)
- [Spring Security JOSE 与 RSA JWT 签发验签](../technologies/Spring-Security-JOSE与RSA-JWT签发验签.md)

---

## 二、合作与学习方式

### 2.1 分工

| 内容 | 默认方式 |
| --- | --- |
| 业务目标、数据结构、事务边界和安全取舍 | 共同讨论后确定 |
| Spring Security、JWT、MySQL 事务、Redis、RocketMQ 等高学习价值内容 | Codex 先整体讲解，用户完成关键部分或明确授权 Codex 实现 |
| DTO、PO、Request、Response、Converter、Fixture 等重复代码 | Codex 批量生成 |
| 领域规则和关键 SQL | 用户理解规则后参与实现，Codex Review |
| 重复测试样板和 Mock 数据 | Codex 生成 |
| 核心行为测试 | 每个里程碑至少由用户亲自完成或解释一个 |
| Git 状态检查、选择性暂存和本地提交 | Codex 负责；push、合并和历史重写另行确认 |

用户可以在任一任务中明确要求 Codex 直接实现。即使由 Codex 编码，开始前仍需说明调用链、关键 API、测试目标和本阶段不做的内容。

### 2.2 每个功能阶段的工作流

1. 检查计划、设计文档、相关代码和 Git 差异。
2. 一次性讲解本阶段的用户结果、调用链、关键接口、数据变化和失败路径。
3. 只确认会影响当前实现的少量决策，不讨论尚未进入本阶段的配置。
4. 按小批次执行 Red → Green → Refactor。
5. Codex 在信息结构稳定后批量补齐重复数据类和测试样板。
6. 运行当前阶段目标测试和必要回归；首次使用某项基础设施时才运行对应集成测试。
7. 选择一至两个与当前阶段直接相关的 MySQL、HTTP、Linux、性能观察或排障练习。
8. Codex 选择性暂存、检查 staged diff 并创建范围清晰的本地提交。
9. 更新本计划中的进度、验证证据和遗留限制。

沟通以“一个完整组件或一个小型纵向切片”为粒度，不再逐个方法或逐个测试来回确认。只有出现新技术选型、范围变化或原因不明的失败时才暂停讨论。

### 2.3 新增复杂度门

引入 Redis、Lua、MQ、Outbox、CDC、分布式锁、缓存或新的基础设施配置前，必须回答：

1. 当前已经出现了什么具体问题？
2. 现有简单实现为什么不能接受？
3. 新机制只解决哪一个主要问题？
4. 用什么测试、日志、故障场景或性能指标证明它有效？

如果只能回答“未来可能需要”或“项目需要展示这项技术”，则先记录为优化候选，不进入当前编码任务。

---

## 三、当前真实进度

### 3.1 已完成

#### 工程骨架

- 父 Maven 工程及 `identity-service`、`gateway-service`、`media-search-service`、`security-contract` 已建立。
- Gateway 已有 `/api/auth/**`、`/api/users/**`、图片和搜索路由骨架。
- Docker Compose 中存在部分超前服务配置，但这些配置不代表相关技术已经选定或完成。

#### 用户注册 I-01

- 用户名、密码、昵称和账户状态领域规则。
- `user_account` 的 Flyway V1 迁移。
- MyBatis-Plus Repository 与 UTC 时间转换。
- BCrypt 密码摘要。
- `POST /api/auth/register`。
- 领域、应用、HTTP 和 MySQL 集成测试。
- 功能提交：`d1feabb feat(identity): implement user registration slice`。

#### Access Token I-02

- RS256 公私钥加载与启动校验。
- `AccessTokenIssuer` 内部端口及 Spring JOSE 实现。
- JWT Header、Claim、TTL、Issuer、Audience 和时钟偏移验证。
- 可复用的 `JwtDecoder` Bean。
- 动态测试密钥与篡改、过期、错误声明等安全测试。
- 功能提交：`6bc0397 feat(identity): implement access token issuance`。
- 文档提交：`80652b1`、`9cf4ec2`。

最近一次 I-02 收口验证执行 `identity-service` 123 项测试，0 失败、0 错误，其中 8 项 MySQL 条件测试因未开启环境开关而跳过。
阶段 1 的登录链路已在 Docker MySQL 临时空库 `identity_test_codex` 上完成真实集成验收。

### 3.2 尚未完成

- 修改密码。
- 账号注销。
- 完整业务接口与真实 Media 用户数据的联动。
- Redis 认证状态、Lua、Outbox 和 RocketMQ 认证消息。

### 3.3 当前最近目标

当前第一目标不是建设最终分布式认证架构，而是完成用户与认证的基础功能闭环：

```text
注册
→ 登录
→ 获得 Access Token
→ 通过 Gateway 访问受保护的 Media 接口
→ Refresh Token 续期
→ 退出后不能再次刷新
→ 查询和修改当前用户资料
→ 修改密码
→ 注销账户
```

其中注册、登录、Gateway/Media 鉴权、Session、Refresh Token、退出当前会话、查询当前用户资料和修改基础资料已经完成；下一步优先完成修改密码与账号注销，随后再进入点赞、评论等业务功能。

---

## 四、功能主线

## 阶段 1：Access Token-only 登录

**状态：已完成**

### 用户结果

已注册且状态正常的用户可以使用用户名和密码登录，并获得一张 30 分钟 Access Token。

### 主流程

```text
POST /api/auth/login
→ 校验请求格式
→ 按大小写敏感用户名查询 user_account
→ 使用 BCrypt 验证原始密码
→ 检查账户状态为 NORMAL
→ 生成随机 sid 和本次 auth_time
→ 调用 AccessTokenIssuer
→ 返回 Access Token、Token 类型和过期时间
```

### 当前数据边界

- 只读取现有 `user_account`。
- 不更新 `last_login_time`。
- 不建立数据库事务。
- `sid` 表示一次登录，使用随机值生成但暂不持久化。
- `auth_time` 是本次用户名密码认证成功时间。
- 使用现有 Scope 常量；不在本阶段设计复杂角色或权限模型。

### 主要失败路径

- 请求字段不合法。
- 用户名不存在。
- 密码错误。
- 账户状态不允许登录。
- Access Token 签发失败。

用户名不存在、密码错误和不可登录状态不得向外暴露账户是否存在、密码摘要或内部状态细节。具体 HTTP 状态和错误码在本阶段开始时统一确认，不提前建立大而全的错误码表。

### 测试重点

- 正确密码登录成功。
- 用户名大小写语义与注册保持一致。
- 错误密码和不存在用户不能签发 Token。
- 非 `NORMAL` 账户不能登录。
- 每次成功登录的 `sid`、`jti` 不同。
- Token 的 `sub` 等于真实用户 ID，并能由现有 `JwtDecoder` 验证。
- 响应、异常和日志不包含密码、密码摘要或完整 Token。

### 明确不做

- Session 表和 Refresh Token 表。
- Refresh Cookie、CSRF 和 Token 轮换。
- Redis 登录失败计数、Lua 或限流。
- Logout、Token 黑名单和即时撤销。
- Outbox、RocketMQ Topic 或认证事件。

### 完成标志

- `POST /api/auth/login` 的应用、HTTP 和 MySQL 集成测试通过。
- 使用注册产生的 BCrypt 摘要能够真实登录。
- 登录返回的 Token 能被 I-02 的 Decoder 验证。

### 实现记录（2026-07-17）

- [x] 新增登录 Command、UseCase、Result、Request、Response，并对密码和 Access Token 的 `toString()` 脱敏。
- [x] Repository 按大小写敏感用户名查询账户，并向应用层转换为最小 `UserAuthentication` 视图。
- [x] 使用 `PresentedPassword` 表达登录凭据，不重复应用当前注册密码策略，避免密码规则演进后阻断旧账户登录。
- [x] `LoginUserService` 完成账户查询、BCrypt 匹配、状态检查、随机 `sid`、固定 Scope 和 Access Token 签发。
- [x] 用户不存在、密码错误和非 `NORMAL` 状态统一返回 `401 AUTH_INVALID_CREDENTIALS`；Token 签发和数据访问故障返回统一 500。
- [x] `identity-service` 定向回归执行 140 项测试，0 失败、0 错误，9 项 MySQL 条件测试跳过。
- [x] 已增加并执行“注册写入 BCrypt → 错误密码 401 → 正确密码登录 → Decoder 回读 JWT”的 MySQL 集成测试。
- [x] Docker MySQL 验收命令：设置 `EPF_TEST_MYSQL_ENABLED=true`，连接临时空库 `identity_test_codex`，执行 `mvn -q "-Dmaven.repo.local=C:\Users\mxl_scut\.m2\repository" -pl identity-service -am "-Dtest=UserRegistrationMySqlIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`；结果为 9 项测试，0 失败、0 错误、0 跳过。
- 环境注意：当前本地 `identity_db` 已存在旧版 Flyway `V1` 执行历史，直接连接会触发 checksum mismatch；本次验收使用临时空库以避免修改开发库的 Flyway 历史。
- 全仓回归已尝试；失败集中在 `media-search-service` 既有 Spring 测试上下文缺少 MyBatis `SqlSessionFactory`，与本次 Identity 登录切片无关，因此没有越界修改媒体模块。
- 环境记录：沙箱中的 Maven 默认本地仓库为 `C:\Users\CodexSandboxOffline\.m2`；本次离线验证显式使用已有 `C:\Users\mxl_scut\.m2\repository`，未修改项目 Maven 配置。

---

## 阶段 2：受保护访问闭环

**状态：进行中，已完成 Gateway 与 Media 的最小 Resource Server 接入，并加入 dev-only 认证探针接口**

### 用户结果

登录用户可以携带 Access Token，通过 Gateway 调用 Media 的受保护接口；未登录或 Token 无效的请求被拒绝。当前因真实搜索、上传链路会牵涉模型服务、Qdrant 和文件处理，优先使用 dev-only 探针接口验证认证闭环，真实业务接口留到基础功能总验收阶段再接入。

### 实施顺序

1. Media 加入最小 Spring Security Resource Server 配置，使用公开公钥本地验签。
2. 划分公开路径和受保护路径；当前使用 `dev` profile 下的 `/api/search/_auth/probe` 作为认证探针，不接入真实搜索或上传流程。
3. 从 `SecurityContext` 中读取 `sub` 和 `scope`，建立最小当前用户访问方式。
4. Gateway 加入相同的 JWT 标准验证与路由授权。
5. Gateway 转发原始 Bearer Token，Media 再次本地验签。
6. 使用自动化测试和一次可复现请求完成跨服务验证。

### 当前安全边界

- Gateway 和 Media 分别验证签名、Issuer、Audience、`nbf` 和 `exp`。
- 两个服务只持有公钥；只有 Identity 持有私钥。
- 首版不查询 Redis、Session 或用户动态状态。
- 首版不根据用户 ID 改造图片所有权，认证接入不能改变现有上传、向量化和搜索流程。

### 测试重点

- 无 Token、篡改 Token、错误签名、错误 Issuer/Audience 和过期 Token 被拒绝。
- 合法 Token 可以通过 Gateway 和 Media 两层验证。
- 绕过 Gateway 直接请求 Media 时，Media 仍会本地验签。
- Scope 不足与身份无效能够区分 403 和 401。

### 完成标志

```text
注册成功
→ 登录成功
→ 获得 Access Token
→ Gateway 验证并转发
→ Media 本地二次验签
→ dev-only 探针接口返回当前认证信息
```

到这里，认证最小闭环完成。Access Token 到期后重新登录是当前明确接受的限制。

### 当前进展记录

- [x] `security-contract` 增加可复用的 JWT audience、必需 Claim 校验和 RSA 公钥 PEM 加载工具。
- [x] Gateway 接入 Spring Security Resource Server，使用 Identity 公钥本地校验 Access Token，并按路由要求 `image:search`、`image:upload`、`image:read`、`user:read`、`user:write`。
- [x] Gateway 路由配置更新为 Spring Cloud Gateway 2025 的 `spring.cloud.gateway.server.webflux.routes` 前缀；阶段 2 不保留 `/api/sessions/**` 路由。
- [x] Media 接入 Spring Security Resource Server，上传、搜索和图片访问分别要求对应图片 scope；`/actuator/health` 与 `/actuator/info` 保持公开。
- [x] 新增 Gateway 集成测试，覆盖公开登录路由、无 Token 401、scope 不足 403、非法 Token 401、合法 Token 路由并转发原始 Bearer Token。
- [x] 新增 Media 安全配置测试，覆盖公开 info、无认证拒绝、scope 不足拒绝、合法 scope 通过。
- [x] 新增 Media dev-only 认证探针 `GET /api/search/_auth/probe`，在 `dev` profile 下返回当前认证主体、认证状态和权限列表；该接口只用于低成本验证 Gateway → Media 鉴权链路，不作为正式业务接口。
- [x] 新增 Media 探针测试，覆盖无 Token 401、scope 不足 403、合法 `image:search` scope 返回认证信息。
- [x] 新增 Gateway 探针路径测试，确认 `/api/search/_auth/probe` 复用 `/api/search/**` 路由和 `image:search` 权限，并转发原始 Bearer Token。
- 验证证据：`mvn -q "-Dmaven.repo.local=C:\Users\mxl_scut\.m2\repository" -pl gateway-service,media-search-service -am "-Dtest=GatewaySecurityIntegrationTest,MediaSecurityConfigurationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` 通过。
- 探针切片验证证据：`mvn -q "-Dmaven.repo.local=C:\Users\mxl_scut\.m2\repository" "-DargLine=-Djdk.attach.allowAttachSelf=true -XX:+EnableDynamicAgentLoading" -pl media-search-service -am "-Dtest=MediaAuthProbeControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` 通过；`mvn -q "-Dmaven.repo.local=C:\Users\mxl_scut\.m2\repository" "-DargLine=-Djdk.attach.allowAttachSelf=true -XX:+EnableDynamicAgentLoading" -pl gateway-service -am "-Dtest=GatewaySecurityIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` 通过。
- 扩展验证：`gateway-service` 全模块测试通过；`media-search-service` 全模块测试仍失败在既有 `ImageAssetMapper` 缺少 `sqlSessionFactory/sqlSessionTemplate` 的 Spring 上下文问题，不是本次安全规则的 401/403 断言失败。

---

## 阶段 3：Session 与 Refresh Token

### 用户结果

用户登录后具有可识别的会话；Access Token 到期时，可以使用 Refresh Token 获取新的 Access Token，而不必重新输入密码。

### 拆分原则

阶段 3 不再一次性做完所有会话能力，拆成两个连续切片：

1. **阶段 3A：登录创建持久化 Session 与首张 Refresh Token。**
2. **阶段 3B：实现 Refresh Token 换发 Access Token，并处理基础轮换。**

这样可以先把登录后的服务端会话锚点建立起来，再围绕这个锚点继续做刷新、退出和撤销，避免在一个任务里同时修改登录、刷新、Cookie、并发轮换和撤销策略。

### 阶段 3A：登录请求与派发链路

#### 当前简单方案

- 使用 Flyway 创建两张表，MySQL 作为唯一权威状态。
- `user_session` 使用随机 `session_id` 作为持久化会话 ID。
- Refresh Token 使用高熵随机值，数据库只保存不可逆摘要。
- 登录事务中创建 Session、保存首张 Refresh Token 摘要并签发 Access Token；如果签发或保存失败，数据库事务回滚，接口不返回 Token。
- Access Token 的 `sid` 使用已经落库的 Session ID。
- 为了保持后端职责清晰，当前登录响应以 JSON 返回 Access Token 和 Refresh Token；Cookie、CSRF/Origin 防护留到明确采用浏览器 Cookie 方案时再补。

#### 测试重点

- 登录成功时创建 Session 与 Refresh Token 摘要，并返回 Access Token 与 Refresh Token 原文。
- 登录失败时不创建 Session、不生成 Refresh Token、不签发 Access Token。
- 数据库不保存 Refresh Token 原文。
- 返回对象与日志输出不泄漏 Access Token 或 Refresh Token。
- MyBatis 映射能正确保存 Session 和 Refresh Token 摘要。

#### 当前进展记录

- [x] 新增 `user_session` 与 `user_refresh_token` Flyway 迁移。
- [x] 登录成功后创建持久化 Session；Session TTL 当前固定为 1 天。
- [x] 登录成功后生成 URL-safe 高熵 Refresh Token；Refresh Token TTL 当前固定为 1 小时。
- [x] 数据库仅保存 Refresh Token 的 SHA-256 摘要，不保存原文。
- [x] 登录响应 JSON 增加 `refreshToken` 与 `refreshTokenExpiresAt`。
- [x] Access Token 的 `sid` 使用持久化 Session ID。
- 验证证据：`mvn -q "-Dmaven.repo.local=C:\Users\mxl_scut\.m2\repository" "-DargLine=-Djdk.attach.allowAttachSelf=true -XX:+EnableDynamicAgentLoading" -pl identity-service -am test` 通过。

### 阶段 3B：Refresh Token 换发链路

#### 当前简单方案

- 实现 `POST /api/auth/refresh`。
- 客户端提交 Refresh Token，服务端计算摘要后查询 MySQL。
- 校验 Token 状态、过期时间和所属 Session 状态。
- 成功后签发新的 Access Token，并立即轮换 Refresh Token。
- 旧 Refresh Token 使用 MySQL 条件更新从 `ACTIVE` 标记为 `USED`，只有影响行数为 1 的请求可以继续插入新 Refresh Token。
- 新 Refresh Token 以 JSON 响应返回，数据库仍只保存 SHA-256 摘要，不保存原文。
- 刷新时 Access Token 的 `auth_time` 沿用原 Session 创建时间，不因为刷新而变成新的认证时间。
- 当前阶段不引入 Cookie/CSRF、Redis、退出登录、撤销链路、MQ 或 Outbox。

#### 测试重点

- 正常刷新成功，旧 Refresh Token 变为 `USED`，响应返回新的 Access Token 和 Refresh Token。
- 同一 Refresh Token 并发使用时不能产生多个有效的新凭据。
- 过期、已使用、已撤销的 Token 不能刷新。
- 不存在、空白或并发复用失败的 Refresh Token 返回 401 `AUTH_INVALID_REFRESH_TOKEN`。

#### 当前进展记录

- [x] 新增 `RefreshTokenUseCase` / `RefreshTokenService`，完成 Refresh Token 换发与轮换编排。
- [x] 新增 `POST /api/auth/refresh`，请求体使用 JSON Refresh Token，响应复用登录 Token 对响应结构。
- [x] Repository 支持按 Refresh Token 摘要查询 Token + Session 权威状态。
- [x] Repository 使用 MySQL 条件更新完成旧 Token `ACTIVE -> USED`，避免并发下重复换发。
- [x] 无效、过期、已使用或并发复用失败统一映射为 401 `AUTH_INVALID_REFRESH_TOKEN`。
- 验证证据：`mvn -q "-Dmaven.repo.local=C:\Users\mxl_scut\.m2\repository" "-DargLine=-Djdk.attach.allowAttachSelf=true -XX:+EnableDynamicAgentLoading" "-Dsurefire.failIfNoSpecifiedTests=false" -pl identity-service -am "-Dtest=RefreshTokenServiceTest,RefreshTokenResultTest,AuthControllerTest,MyBatisUserRefreshTokenRepositoryTest" test` 通过。

### 阶段 3 明确不做

- Redis Session 缓存或 deny Key。
- Lua、Outbox、RocketMQ 和跨存储补偿。
- 多设备管理界面和指定设备下线。

---

## 阶段 4：基础退出与登录撤销

### 用户结果

用户可以退出当前会话；退出后不能继续使用该会话的 Refresh Token 获取新 Access Token。

### 当前简单方案

- 实现 `POST /api/auth/logout`。
- 客户端携带 `Authorization: Bearer <access-token>`；Identity Service 使用现有 `JwtDecoder` 验签并读取 `sub` 和 `sid`。
- 在一个 MySQL 事务中撤销当前 Session 及其 ACTIVE Refresh Token。
- 当前阶段 Refresh Token 通过 JSON 返回和提交，不使用 Refresh Cookie；客户端退出时删除本地保存的 Access Token 与 Refresh Token。
- 重复退出返回稳定结果，不恢复已经撤销的状态。
- 当前阶段不接入 Redis、MQ、Outbox 或跨服务事件通知。

### 当前明确限制

基础版本不查询 Redis deny 状态，因此退出前已经签发的 Access Token 最长仍可使用至 30 分钟 TTL 到期。客户端退出时应立即删除本地 Access Token；服务端即时拒绝旧 Access Token 属于后续优化，而不是本阶段的隐藏完成条件。

当前 Gateway 已对 `POST /api/auth/logout` 单独要求认证，未携带有效 Access Token 的退出请求会在 Gateway 被 401 拦截，不会路由到 Identity Service。Identity Service 仍会再次解码 Bearer Access Token 并读取 `sub` 与 `sid`，作为服务自身的安全边界。

### 测试重点

- 退出事务正确撤销 Session 和 Refresh Token。
- 退出后刷新失败。
- 重复退出保持幂等。
- 其他 Session 不受当前 Session 退出影响。

#### 当前进展记录

- [x] 新增 `LogoutCurrentSessionUseCase` / `LogoutCurrentSessionService`，完成当前 Session 退出编排。
- [x] 新增 `POST /api/auth/logout`，从 Bearer Access Token 解析 `sub` 和 `sid` 后撤销。
- [x] `user_session` 使用 MySQL 条件更新 `ACTIVE -> REVOKED`，并校验 `session_id + user_id`。
- [x] `user_refresh_token` 按 `session_id + ACTIVE` 批量更新为 `REVOKED`。
- [x] Access Token 缺失、格式错误、验签失败或必要 Claim 缺失统一映射为 401 `AUTH_INVALID_ACCESS_TOKEN`。
- [x] Gateway 将 `POST /api/auth/logout` 从 `/api/auth/**` 公开规则中提前分流为 authenticated 路由；登录、注册和刷新仍保持公开入口。
- 验证证据：`mvn -q "-Dmaven.repo.local=C:\Users\mxl_scut\.m2\repository" "-DargLine=-Djdk.attach.allowAttachSelf=true -XX:+EnableDynamicAgentLoading" "-Dsurefire.failIfNoSpecifiedTests=false" -pl identity-service -am "-Dtest=LogoutCurrentSessionServiceTest,LogoutCurrentSessionCommandTest,AuthControllerTest,MyBatisUserSessionRepositoryTest,MyBatisUserRefreshTokenRepositoryTest" test` 通过。
- Gateway 验证证据：`mvn -q "-Dmaven.repo.local=C:\Users\mxl_scut\.m2\repository" "-DargLine=-Djdk.attach.allowAttachSelf=true -XX:+EnableDynamicAgentLoading" "-Dsurefire.failIfNoSpecifiedTests=false" -pl gateway-service -am "-Dtest=GatewaySecurityIntegrationTest" test` 通过。

---

## 阶段 5：用户信息与账户操作

**状态：进行中，已完成当前用户资料查询与基础资料修改**

### 用户结果

登录用户可以查看和修改自己的基础信息、修改密码并注销账号。

### 实施顺序

1. `GET /api/users/me`：查询当前用户，计算昵称回退后的展示名。
2. `PATCH /api/users/me/profile`：修改昵称等首版资料。
3. `POST /api/users/me/password`：验证旧密码、保存新摘要、推进 `auth_valid_after` 并撤销已有 Session。
4. `DELETE /api/users/me`：逻辑注销、释放原用户名并撤销已有 Session。

### 当前简单方案

- 用户信息直接查询 MySQL，不建立资料缓存。
- 资料修改只更新账户表，不发布事件。
- 当前已落地字段为 `nickname` 与 `avatarUrl`；`displayName` 由服务端计算，昵称为空时回退为 `username`。
- 修改密码和注销使用 MySQL 事务更新用户与 Session 权威状态。
- 旧 Access Token 仍遵循阶段 4 的自然过期限制；即时全局失效留到优化阶段。
- 没有真实下游消费者时，不创建用户生命周期 Topic 或 Outbox 事件。

### 测试重点

- 用户只能操作自己的资料。
- 昵称为空时回退显示用户名。
- 修改密码后旧密码不能再次登录，已有 Refresh Token 不能刷新。
- 注销后账户不能登录，原用户名可以重新注册。
- 密码、Token、Cookie 和内部摘要不进入日志或响应。

### 当前进展记录

- [x] 新增 `GET /api/users/me`，从 Bearer Access Token 的 `sub` 解析当前用户 ID，查询 `user_account` 中的基础资料。
- [x] 新增 `PATCH /api/users/me/profile`，支持修改或清空昵称、头像 URL；空昵称展示名回退为用户名。
- [x] 当前资料接口只对 `NORMAL` 用户返回资料；用户不存在或不可用返回 `404 USER_PROFILE_NOT_FOUND`。
- [x] Repository 支持按用户 ID 读取资料，并使用 MyBatis-Plus 条件更新当前用户资料和 `updated_time`。
- 验证证据：`mvn -q "-Dmaven.repo.local=C:\Users\mxl_scut\.m2\repository" "-DargLine=-Djdk.attach.allowAttachSelf=true -XX:+EnableDynamicAgentLoading" "-Dsurefire.failIfNoSpecifiedTests=false" -pl identity-service -am "-Dtest=UserProfileServiceTest,UserControllerTest,MyBatisUserRepositoryTest" test` 通过。
- 回归证据：`mvn -q "-Dmaven.repo.local=C:\Users\mxl_scut\.m2\repository" "-DargLine=-Djdk.attach.allowAttachSelf=true -XX:+EnableDynamicAgentLoading" -pl identity-service -am test` 通过；`mvn -q "-Dmaven.repo.local=C:\Users\mxl_scut\.m2\repository" "-DargLine=-Djdk.attach.allowAttachSelf=true -XX:+EnableDynamicAgentLoading" -pl gateway-service -am test` 通过。

---

## 阶段 6：基础功能总验收

这一阶段不增加新架构，只修复主链路暴露的问题并完成整体回归。

### 对外接口基线

```text
POST   /api/auth/register
POST   /api/auth/login
POST   /api/auth/refresh
POST   /api/auth/logout
GET    /api/users/me
PATCH  /api/users/me/profile
POST   /api/users/me/password
DELETE /api/users/me
```

Session 列表、指定设备下线、退出全部设备和管理员禁用/启用不是第一轮基础闭环的完成条件，出现明确产品需求后分别增加。

### 完整验收链路

```text
注册
→ 登录
→ Gateway/Media 受保护访问
→ Access Token 过期后刷新
→ 退出后不能再次刷新
→ 重新登录
→ 查看和修改资料
→ 修改密码并重新认证
→ 注销账户并释放用户名
```

### 收口要求

- 新功能的单元、HTTP 和必要集成测试通过。
- 401、403、400、409 和 500 的职责清晰，敏感内容不泄漏。
- 文档与当前代码行为一致，不把优化候选写成已有能力。
- 记录一次完整手工调用过程和关键排障命令。
- 完成阶段性 Git 提交和回归基线。

---

## 五、功能完成后的优化支线

本节只记录演进方向，不在当前阶段提前展开表结构、Key、Lua、Topic、消息字段或组件配置。

| 真实问题或目标 | 候选方案 | 进入编码前需要验证 |
| --- | --- | --- |
| 登录失败请求过多或存在暴力尝试 | Redis 失败计数、临时限制、接口限流 | 阈值、窗口、误伤范围、并发正确性 |
| 退出后必须立即拒绝旧 Access Token | Redis `sid deny`，Gateway 请求时检查 | 撤销延迟、TTL、Redis 故障策略 |
| 修改密码、禁用或注销后必须立即全局失效 | 用户认证状态缓存、`auth_valid_after` 检查 | 状态传播延迟、旧 Token 窗口 |
| MySQL 状态提交后 Redis 更新失败不可接受 | 短期屏障和可靠补偿 | 失败窗口、恢复路径、重复执行 |
| 出现必须可靠传播的真实跨服务事件 | Outbox + RocketMQ | 数据库原子写、重复投递、幂等消费、重试 |
| 应用扫描 Outbox 成为瓶颈或运维负担 | Debezium/CDC 作为候选 Relay | Binlog、部署成本、投递语义、故障恢复 |
| 多设备管理成为真实需求 | Session 列表、指定设备下线、退出全部设备 | 设备信息、隐私、并发撤销 |
| 公钥分发或无停机轮换成为需求 | `kid`、JWK Set、多密钥轮换 | 旧 Token 保留期、缓存和回滚 |
| 用户资料查询成为热点 | Redis Cache-Aside | 命中率、陈旧容忍、数据库 QPS |
| 接口性能不达标 | 索引、连接池、缓存、批处理或扩容 | 串行压测、P95/P99、CPU/内存/数据库证据 |

### RocketMQ 与 Outbox 边界

- 项目消息队列固定使用 RocketMQ，不再引入 Kafka 名称或 Kafka 专用语义。
- 没有真实生产者和消费者时，不创建 Topic、Consumer Group、消息 DTO 或 Outbox 表。
- Outbox Relay 使用应用扫描还是 Debezium/CDC，不在本计划中预选。
- 当某个真实业务事件进入编码阶段时，先补充消息用途、生产者、消费者、重复投递、幂等、重试和故障恢复，再由用户确认 Relay 方案。

### 优化任务的固定方法

每次只加入一种主要机制，并留下改造前后的对比：

```text
当前问题
→ 基线数据或故障现象
→ 候选方案与取舍
→ 只实现选定机制
→ 自动化测试或故障演练
→ 性能/一致性结果对比
→ 决定保留、调整或回退
```

---

## 六、当前有效决策摘要

- 用户 ID：MySQL `BIGINT AUTO_INCREMENT`。
- 用户名：大小写敏感，3～32 位，只允许英文字母、数字和规则内下划线。
- 密码输入：6～25 个 Unicode Code Point、UTF-8 不超过 72 字节、无空白和控制字符。
- 密码摘要：Spring Security `DelegatingPasswordEncoder` + BCrypt，保存 `{bcrypt}` 前缀。
- 账户状态：`NORMAL、DISABLED、DELETED`。
- 数据库迁移：Flyway。
- 时间：Java `Clock` 生成 UTC `Instant`，MySQL 使用 `DATETIME(3)`。
- Access Token：RS256、30 分钟 TTL、30 秒 Clock Skew。
- JWT：`sub` 为用户 ID，`sid` 为登录/Session ID，包含既定 `iss、aud、jti、scope、auth_time、iat、nbf、exp`。
- 当前单密钥方案不使用 `kid`，不提供 JWK Set。
- 消息队列固定为 RocketMQ，但基础功能阶段不使用消息队列。
- Compose 中已经存在的 Redis、RocketMQ、Debezium 或 CDC 配置不自动成为实现结论。

未在本摘要中列出的字段和边界以相关设计文档及已经通过测试的代码为准；真正影响当前切片但仍未确定的事项，在该切片开始时集中讨论。
