# EveryPicFound

EveryPicFound 是一个多模态搜图项目，核心链路为：

```text
图片上传与管理 → 图片向量化 → 向量入库 → 图搜图 / 文搜图 / 图文混搜
```

当前仓库正在从单体后端演进为微服务结构。用户与认证能力采用短期 JWT、持久化 Session、Refresh Token 轮换、MySQL 权威状态和 Redis 在线认证状态的设计。

## 模块

| 模块 | 职责 |
| --- | --- |
| `identity-service` | 用户账户、登录、Token、Session、认证状态与后续 Outbox 能力 |
| `gateway-service` | 对外入口、路由、JWT 校验、动态认证状态检查与授权 |
| `media-search-service` | 图片资产、向量化、搜索；作为 Resource Server 二次验签 |
| `security-contract` | 跨服务共享的 JWT Claim、Scope 和必要内部契约 |
| `modelservice` | Python/FastAPI 图片与文本向量化服务 |
| `everypicfound-frontend` | 前端页面 |
| `deploy` | 运行环境相关配置，例如 RocketMQ Broker 配置 |

## 当前阶段

微服务工程、模块边界、最小配置和容器边界已经建立。用户与认证业务尚未开始编码，因此当前不创建用户认证表、Redis 业务 Key、Lua 脚本、RocketMQ Topic、Outbox 事件或 RSA 密钥。

运行环境、Docker Compose、旧测试和构建基线会在对应功能首次实际使用时分别验证；不要把当前 Compose 中的 Debezium/CDC 配置视为已经选定的 Outbox 实现方案。

## 开发入口

优先阅读以下文档：

1. [文档索引与维护规范](docs/README.md)
2. [用户与认证模块设计](docs/modules/EveryPicFound_用户与认证模块设计.md)
3. [JWT 登录认证与会话管理技术设计](docs/modules/基于JWT的登录认证与会话管理技术设计文档.md)
4. [用户与认证模块编码计划](docs/modules/EveryPicFound%20用户与认证模块编码计划.md)
5. [准备阶段收口记录](docs/modules/用户认证准备阶段收口清单.md)
6. [模块设计文档](docs/project/模块设计文档.md)

当前用户与认证开发采用“学习核心、生成样板”的协作方式：核心配置、事务、SQL、Redis Lua、RocketMQ、测试和排障由开发者主导；DTO、PO、Converter、测试夹具等重复代码可由 Codex 批量生成并协助检查。

## 运行原则

- 只有 Gateway 对公网开放；内部服务使用容器网络服务名互通。
- 消息队列固定使用 RocketMQ；Topic 在首次有真实生产者和消费者时创建。
- 每个功能切片先确认数据变化和技术选择，再增加对应依赖、配置、迁移与测试。
- 性能测试严格串行：单脚本、单场景、单时间窗、单批次。
