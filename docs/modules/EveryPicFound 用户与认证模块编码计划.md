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

- Identity、Gateway 和 Media 的 Resource Server 安全链。
- 携带 Access Token 访问受保护业务接口。
- Session 和 Refresh Token。
- Token 刷新与退出登录。
- 当前用户资料、修改密码和账号注销。
- Redis 认证状态、Lua、Outbox 和 RocketMQ 认证消息。

### 3.3 当前最近目标

第一目标不是建设最终会话架构，而是完成：

```text
注册
→ 登录
→ 获得 Access Token
→ 通过 Gateway 访问受保护的 Media 接口
→ Gateway 与 Media 正确识别用户身份
```

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

### 用户结果

登录用户可以携带 Access Token，通过 Gateway 调用现有图片上传或搜索接口；未登录或 Token 无效的请求被拒绝。

### 实施顺序

1. Media 加入最小 Spring Security Resource Server 配置，使用公开公钥本地验签。
2. 划分公开路径和受保护路径，不新建仅用于演示认证的伪业务接口。
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
→ 上传或搜索接口返回业务结果
```

到这里，认证最小闭环完成。Access Token 到期后重新登录是当前明确接受的限制。

---

## 阶段 3：Session 与 Refresh Token

### 用户结果

用户登录后具有可识别的会话；Access Token 到期时，可以使用 Refresh Token 获取新的 Access Token，而不必重新输入密码。

### 当前简单方案

- 本阶段开始时再确认 `user_session` 和 `user_refresh_token` 的最小字段。
- 使用 Flyway 创建两张表，MySQL 作为唯一权威状态。
- Refresh Token 使用高熵随机值，数据库只保存不可逆摘要。
- 登录事务同时创建 Session 和首张 Refresh Token；事务提交后再签发 Access Token。
- Access Token 的 `sid` 改为持久化 Session ID。
- 实现 `POST /api/auth/refresh`。
- Refresh Token 每次成功使用后轮换，使用 MySQL 条件更新保证并发请求只有一个成功。
- Cookie 属性、CSRF/Origin 和轮换 TTL 在本阶段编码前集中确认。

### 测试重点

- 登录事务失败时不能返回任何 Token。
- 数据库不保存 Refresh Token 原文。
- 正常刷新成功并轮换旧 Token。
- 同一 Refresh Token 并发使用时只有一个请求成功。
- 过期、已使用、已撤销的 Token 不能刷新。

### 明确不做

- Redis Session 缓存或 deny Key。
- Lua、Outbox、RocketMQ 和跨存储补偿。
- 多设备管理界面和指定设备下线。

---

## 阶段 4：基础退出与登录撤销

### 用户结果

用户可以退出当前会话；退出后不能继续使用该会话的 Refresh Token 获取新 Access Token。

### 当前简单方案

- 实现 `POST /api/auth/logout`。
- 在一个 MySQL 事务中撤销当前 Session 及其 ACTIVE Refresh Token。
- 清理客户端 Refresh Cookie。
- 重复退出返回稳定结果，不恢复已经撤销的状态。
- Refresh Token 重放先只影响其所属 Session。

### 当前明确限制

基础版本不查询 Redis deny 状态，因此退出前已经签发的 Access Token 最长仍可使用至 30 分钟 TTL 到期。客户端退出时应立即删除本地 Access Token；服务端即时拒绝旧 Access Token 属于后续优化，而不是本阶段的隐藏完成条件。

### 测试重点

- 退出事务正确撤销 Session 和 Refresh Token。
- 退出后刷新失败。
- 重复退出保持幂等。
- 其他 Session 不受当前 Session 退出影响。

---

## 阶段 5：用户信息与账户操作

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
- 修改密码和注销使用 MySQL 事务更新用户与 Session 权威状态。
- 旧 Access Token 仍遵循阶段 4 的自然过期限制；即时全局失效留到优化阶段。
- 没有真实下游消费者时，不创建用户生命周期 Topic 或 Outbox 事件。

### 测试重点

- 用户只能操作自己的资料。
- 昵称为空时回退显示用户名。
- 修改密码后旧密码不能再次登录，已有 Refresh Token 不能刷新。
- 注销后账户不能登录，原用户名可以重新注册。
- 密码、Token、Cookie 和内部摘要不进入日志或响应。

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
