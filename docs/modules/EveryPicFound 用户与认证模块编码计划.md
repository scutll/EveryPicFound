# EveryPicFound 用户与认证模块编码计划

## 零、教学型协作与执行规则

### 0.1 项目学习目标

本项目不仅以完成功能为目标，还用于系统学习微服务、Spring Boot、Spring Security、MySQL、Redis、RocketMQ、测试、可观测性、Linux 与 Git。

开发效率和学习效果按以下原则平衡：

- 用户亲自完成高学习价值的设计、配置、核心逻辑、测试与排障。
- Codex 负责 DTO、PO、Converter、Builder、Fixture 等重复性代码，并提供讲解、提示、Review 和验证支持。
- 不追求一次性完成整个模块；每次只推进一个能够独立理解、测试和提交的纵向切片。
- 每个切片都要留下可运行代码、自动化测试、观测证据、Linux 操作记录和清晰的 Git 提交。

### 0.2 角色分工

| 工作内容 | 默认负责人 | 执行要求 |
| --- | --- | --- |
| 用例、数据流、模块边界和事务边界 | 共同决策 | 用户先表达判断，Codex 补充遗漏和方案取舍 |
| Spring Security、JWT、Redis、RocketMQ 核心配置 | 用户主导 | Codex 先讲原理和关键接口，再由用户编写 |
| 关键 SQL、索引、条件更新和并发控制 | 用户主导 | 用户说明正确性依据，Codex Review |
| Redis Lua、Outbox、消息幂等和故障补偿 | 用户主导 | 先画清状态变化，再编码和故障验证 |
| Controller、DTO、PO、Converter、简单枚举 | Codex 生成 | 用户确认字段语义、边界和命名 |
| Mapper、Repository 的机械性代码 | Codex 生成骨架 | 关键查询、条件更新和事务语义由用户完成 |
| 核心单元测试 | 用户主导 | 每个切片至少亲自完成一个关键行为测试 |
| Fixture、Builder、Mock 数据和重复测试样板 | Codex 生成 | 不在生产代码中加入仅供测试使用的方法 |
| 集成测试、并发测试和故障场景 | 共同完成 | 用户先说明预期，Codex 协助搭建和核验 |
| 日志、指标、性能结果和故障输出分析 | 用户先分析 | Codex 基于证据补充，不直接跳到修复 |
| Linux 与 Git 操作 | 用户实际执行 | Codex 解释命令、预期结果和风险 |

用户可以明确说“直接实现”来临时授权 Codex 编写核心代码；未获得该指令时，核心学习任务默认由用户先尝试。

### 0.3 每个编码切片的固定流程

每个任务必须按以下顺序推进：

1. **读取上下文**：Codex 先检查本计划、设计文档、相关代码和当前 Git 差异。
2. **学习导入**：Codex 说明本切片的目标、关键知识、调用链、前置条件和明确不做的内容。
3. **信息结构确认**：先确定接口、字段、状态变化、错误语义、事务边界和并发策略，再创建代码或基础设施对象。
4. **技术决策门**：文档未锁定的方案必须列出候选项、取舍和推荐，由用户确认后再引入依赖或配置。
5. **任务分工**：当前切片拆成 `[共同决策]`、`[你实现]`、`[Codex 生成]`、`[验证]`、`[观测与 Linux]`、`[Git]`。
6. **测试先行**：先写一个会因目标行为尚未实现而失败的测试，确认失败原因正确后再写最小实现。
7. **核心实现**：用户完成高学习价值代码；Codex 采用“原理提示 → 相关接口 → 伪代码 → 局部示例 → 完整实现”的渐进提示。
8. **样板补齐**：核心接口和信息结构稳定后，Codex 批量生成重复数据类、转换器和测试夹具。
9. **目标验证**：优先运行当前切片的目标测试；首次使用 MySQL、Redis 或 RocketMQ 时才增加对应集成验证。
10. **观测与排障**：检查一次成功路径和一次失败路径的日志、指标或基础设施状态，并由用户先解释结果。
11. **Linux 练习**：每个切片至少练习一组与当前功能直接相关的命令。
12. **Git 收口**：用户执行 `status → diff → stage → staged diff → commit`，并说明提交边界和提交信息。
13. **学习复盘**：记录本次掌握内容、失败原因、使用命令和仍需复习的问题。

为减少不必要的对话往返，编码阶段采用“批量讲解、组内 TDD、批次 Review”的沟通粒度：Codex 一次说明一个领域对象或一个小型组件的完整行为矩阵、所需 API/注解、测试结构和参考实现；用户在本地仍按测试先行顺序观察 RED 与 GREEN，但无需逐个测试回传，完成整组后统一提交代码和测试输出供 Review。涉及新技术决策、失败原因不明或需要改变边界时，才暂停批次继续讨论。

### 0.4 测试规则

- 新业务行为遵循 Red → Green → Refactor；没有观察到正确的失败，不进入实现。
- 用户每个切片至少亲自编写一个关键测试，Codex 可以生成重复夹具和边缘场景样板。
- 单元测试优先验证领域规则和应用编排，不通过大量 Mock 复述实现细节。
- 集成测试只在首次真实使用相应基础设施时加入，避免准备阶段提前搭建所有容器。
- 旧测试作为独立清理任务处理，不为迁就失效测试而扭曲新设计。
- 性能测试严格串行：单脚本、单场景、单时间窗、单批次；结果落盘后才能开始下一项。

### 0.5 观测、Linux 与 Git 学习规则

每个切片绑定一个实际练习主题：

- MySQL：连接数据库、查看表结构、索引、执行计划、事务和锁状态。
- Redis：查看 Key 类型、字段、TTL、Lua 返回值和慢操作信息。
- RocketMQ：查看 Topic、Consumer Group、消息重试和消费进度。
- HTTP：使用 `curl` 检查请求头、Cookie、状态码和响应体。
- 服务排障：使用 `ps`、`ss`、`top`、`tail`、`grep`、`docker logs`、`docker exec` 等命令定位进程、端口和日志。
- Git：练习小提交、选择性暂存、提交历史、分支、冲突与回归定位。

Codex 在给出命令前必须说明用途、预期输出和风险；用户执行后先解释观察结果。Codex 不主动执行 `commit`、`push`、合并、历史重写或破坏性 Git 操作，除非用户明确授权。

### 0.6 Codex 必须遵守的行为边界

- 不一次性代替用户完成整个编码任务，也不在未讲解前直接落地核心技术代码。
- 不擅自引入设计文档和本计划之外的框架、中间件或基础设施方案。
- 遇到未确定方案时先给出可选方案、适用条件和代价，再等待用户确认。
- 不提前创建尚无真实生产者、消费者或用例的表、缓存、Lua、Topic、事件和配置。
- 不把 DTO、PO、数据库记录或 Redis 实现泄漏到不应依赖它们的模块。
- 修改代码前说明将改哪些文件及原因；修改后报告验证证据和仍未验证的内容。
- 保留工作区中用户已有的改动，不覆盖、不回滚、不顺手整理无关文件。
- 诊断失败时先收集错误输出、日志和最小复现，再判断原因；不靠猜测连续试改。
- 不用健康检查代替启动命令验证，不在 Windows 上临时拼接未经验证的后台启动命令。
- 未经用户明确授权，不创建 Git 提交、推送远端、修改历史或执行破坏性命令。

### 0.7 当前切片任务卡模板

开始每个编码任务前，Codex 必须在对话中按以下格式展开当前任务；需要长期保留的关键决策同步回写本计划或设计文档。

```markdown
#### 任务：<编号与名称>

**学习目标**
- <本次需要掌握的知识>

**共同决策**
- [ ] <数据结构、事务或技术选择>

**你实现**
- [ ] <核心配置、规则、SQL、Lua、测试或排障>

**Codex 生成**
- [ ] <DTO、PO、Converter、Fixture 等重复代码>

**验证**
- [ ] <目标测试及预期结果>

**观测与 Linux**
- [ ] <日志、指标、数据库、中间件或命令练习>

**Git**
- [ ] 检查工作区和差异
- [ ] 选择性暂存并检查 staged diff
- [ ] 用户确认提交信息后执行提交

**复盘**
- 掌握内容：
- 失败与原因：
- 使用命令：
- 待复习问题：
```

### 0.8 单个切片完成标准

只有同时满足以下条件，当前切片才标记完成：

- 用户能够说明请求数据流、权威状态、事务边界、主要失败路径和并发保证。
- 目标单元测试和本切片需要的集成测试通过，并保留可复现命令。
- 成功与失败路径至少有一种可观察证据，且日志不包含密码、Token 或 Cookie 等敏感数据。
- 完成与当前切片相关的 Linux 或基础设施命令练习。
- Git 差异只包含本切片内容，提交信息能够准确说明行为变化。
- 新增的技术决策、接口或消息契约已经同步更新文档。

### 0.9 计划进度维护规则

本文件是用户与认证模块后续协作的执行基线。编码过程中按以下方式维护：

- 同一时间只允许一个编码切片处于“进行中”，未完成当前切片前不并行展开下一切片。
- 开始切片前，先在对应阶段下补充或更新任务卡，确认学习目标、分工、技术决策门和验收方式。
- 使用 `- [ ]`、`- [x]` 记录任务状态；没有目标测试或其他约定证据时，不得仅凭代码已写完勾选完成。
- 遇到文档未确定的组件通信、流程节点或实现工具时，将其记为“待决策”，用户确认且设计文档同步后才能编码。
- 若实现过程中需要改变切片范围、依赖或技术方案，先更新计划和原因，再继续修改代码。
- 每个切片收口时记录验证命令与结果、观测结论、Linux 练习、遗留问题；若用户执行了提交，再记录对应提交号。
- 准备阶段已经超前存在的 Docker 服务或配置不自动转化为技术结论，只有在实际切片完成方案确认后才纳入验收。

## 一、现状与进度判断

  - **准备阶段最小收口已完成**：父 Maven 工程、`identity-service`、`gateway-service`、`media-search-service`、`security-contract`、启动类、Dockerfile、基础路由、版本约束和编码准入边界已经落地。
  - **包结构已基本搭好，但尚未进入业务编码**：`identity-service` 目前除启动类外主要是 53 个 `package-info.java`；Gateway 同样只有骨架；Media 的安全模块也只有包占位。
  - **公共契约仅有初稿**：JWT Claim、Scope 和 `UserAuthStateResponse` 已存在，但仍需在首次真实调用前结合接口信息结构重新确认，不能视为稳定 API。
  - **业务能力尚未开始**：没有用户、Session、Refresh Token、Outbox 表，没有 Redis 认证 Key/Lua，没有 RSA 密钥，没有 RocketMQ 业务 Topic，也没有注册、登录、刷新、退出等实现。
  - **Compose 已超前配置**：RocketMQ、Debezium、CDC、MySQL Binlog 等已经写入，但未运行、未验证，也不作为后续编码的既定方案。RocketMQ Broker 已关闭自动建 Topic，符合“实际使用时再建立”的原则。
  - **当前工作区存在未提交骨架改动**，正式编码前应先确认并独立保存这些基础结构，避免和首个业务切片混在一起。
  - 准备阶段收口未运行 Docker、全量 Maven 构建或旧测试；进入编码阶段后按当前纵向切片执行目标验证。

  ## 二、模块协作原则

  用户与认证仍部署在同一个 `identity-service`，但按职责分为两个逻辑工作流：

  - **用户域**拥有账户 ID、用户名、密码摘要、昵称、账户状态和 `auth_valid_after`。
  - **认证域**拥有 JWT、Session、Refresh Token、登录保护和撤销状态。
  - 认证域通过最小用户认证视图读取 `userId、passwordHash、status、authValidAfter`，不调用用户 Controller，也不复制用户模型。
  - 注册负责产生稳定 `userId`；登录以该 ID 创建 Session 并写入 JWT `sub`；`CurrentUser` 再把 `sub` 转回可信用户 ID。
  - 改密、注销等跨域用例由应用层统一编排用户状态、Session、Refresh Token、Outbox 和 Redis 屏障，避免用户模块与认证模块互相调用 HTTP 接口或形成循环依赖。
  - 开发采用交叉纵向切片：账户基础与 Token 基础并进，在登录处汇合；资料与 Session 能力继续分别推进，在改密和注销处再次汇合。

  ## 三、重新安排后的执行阶段

  ### 阶段 0：准备阶段收口（已完成）

  - 保留现有模块、Dockerfile、包边界和最小配置，不继续创建业务配置类、数据库表、Redis Key、Lua、RSA 密钥或 Topic。
  - Gateway 使用 `/api/auth/**` 统一路由认证服务并覆盖 `/api/auth/sessions/**`，已移除错误且重复的 `/api/sessions/**` 路由。
  - 复核 `security-contract`：只保留已存在真实消费者的 Claim、Scope 和内部接口 DTO；事件消息类型在 RocketMQ 消费者出现时再加入。
  - 依赖按功能切片引入：准备阶段不一次性加入 MyBatis、Redis、JOSE、RocketMQ、数据库迁移等全部依赖。
  - Checkstyle、SpotBugs、PMD、JaCoCo 和数据库迁移工具均未由两篇设计文档锁定，不把它们作为准备阶段阻塞项。
  - 现有 Debezium/CDC Compose 配置保持未验证、非权威状态，暂不继续完善或据此设计 Outbox 表。

  ### 阶段 1：信息结构确认与两条基础线并进

  #### 阶段 1 决策记录（持续更新）

  - [x] **D01 用户 ID**：使用 MySQL `BIGINT AUTO_INCREMENT`；当前不引入分布式 ID。
  - [x] **D02 用户名规范**：首尾先执行 `strip()`，内部不允许任何空白；规范化后的用户名长度为 3～32，只允许大小写英文字母、数字和下划线；大小写敏感，`User01` 与 `user01` 是不同账号，后端不转换大小写；下划线只能位于字母或数字段之间，不能位于首尾或连续出现；数据库唯一索引必须采用与该语义一致的大小写敏感 Collation。
  - [x] **D03 昵称规范**：`nickname` 允许为 `NULL`，注册时不要求填写，也不把用户名复制到昵称字段；对外展示名称按“非空昵称优先，否则回退到 username”计算；用户主动设置时先执行 `strip()`，规范化后的长度为 1～32 个 Unicode Code Point，允许普通可打印 Unicode（包括中文、常见符号和 Emoji），拒绝换行与控制字符；清空后保存为 `NULL`。
  - [x] **D04 密码输入策略**：长度 6～25，不强制字符组合，不允许空白字符。该选择低于 NIST 单因素密码建议，标记为学习环境基线，上线前必须重新进行安全评审。
  - [x] **D05 密码哈希算法**：使用 BCrypt，不使用 RS256；具体工作因子在编码切片中通过本机耗时测试确定，并保留未来算法升级能力。
  - [x] **D06 账户状态**：首版只使用 `NORMAL、DISABLED、DELETED`，不建立 `LOCKED`；登录失败次数限制由后续 Redis 临时锁表达，不能把临时锁写成 `DISABLED`。
  - [x] **D07 认证分界时间**：账户创建时 `auth_valid_after = created_time`；后续仅在修改密码、禁用/启用、注销、退出全部设备等需要使既有认证整体失效的安全事件中推进，不随昵称等普通资料更新而改变。
  - [x] **D08 账户表与用户名释放**：首版加入 `avatar_url、version、last_login_time`；账户注销事务中将 `username` 改为 `#deleted#<userId>` 并将状态设为 `DELETED`，原用户名随即可以重新注册，唯一索引继续约束现存字段值。已注销账户以稳定的 `userId` 精确定位；前缀查询只用于后台管理排查，不作为业务唯一查询条件或账户恢复标识。
  - [x] **D09 数据库迁移**：采用 Flyway 管理版本化 SQL；用户仍负责关键 DDL、索引和约束设计。
  - [x] **D10 注册用例边界**：采用独立 `RegisterUserUseCase` 与实现服务，由 `AuthController` 调用；保持用例级粒度，不为每个简单步骤拆接口。
  - [x] **D11 注册输入校验错误契约**：用户名和注册密码违反领域规则时返回 HTTP 400；领域层分别使用 `UsernameViolation` 与 `PasswordViolation` 表达结构化原因，接口层映射为稳定的字符串 `USER_*` 错误码，并返回 `errorCode、message、field`。客户端依赖 `errorCode` 而非异常消息；异常消息和密码原文不得进入响应或业务日志。
  - [x] **D12 注册其余错误契约**：用户名重复返回 HTTP 409、`USER_USERNAME_ALREADY_EXISTS`、`field=username`；密码哈希失败、数据库失败等客户端无法处理的内部故障统一返回 HTTP 500、`SYSTEM_INTERNAL_ERROR`、`field=null`。服务端依靠异常类型和日志区分内部原因，不向客户端暴露 BCrypt、MySQL 或堆栈细节。
  - [x] **D13 依赖引入**：依赖只在当前编码切片首次真实使用时加入，不一次性补齐后续依赖。
  - [x] **D14 数据库验证**：使用现有 Docker MySQL 建表和执行集成测试，不引入 H2 或 Testcontainers。
  - [ ] **D15 JWT Claim 契约**：遵循 IETF JWT/JWS/JWK 规范；保留 `nbf` 且首版令 `nbf = iat`，其余字段在 Token 切片开始前逐项核对。
  - [x] **D16 时间参数**：Access Token TTL 30 分钟、Refresh Token TTL 1 小时、Session 绝对 TTL 1 天、Clock Skew 30 秒；Refresh Token 是否采用滑动到期在轮换设计时再确认。
  - [x] **D17 RSA 密钥基线**：RS256、RSA 2048 位、PKCS#8 PEM 私钥、X.509 PEM 公钥；开发密钥在本地生成且不提交 Git，路径由外部配置注入，测试使用独立密钥。
  - [ ] **D18 `kid` 与密钥轮换**：延期到 JWT 签发切片，在讲解签名、验签、JWK Set 和轮换流程后确认，不能直接按默认值编码。
  - [ ] **D19～D21 Refresh Token、Cookie/CSRF 与敏感配置**：进入对应 Token 切片后逐项讨论。
  - [x] **D22 首版账户表边界**：采用单张 `user_account`，字段限定为 `id、username、password_hash、nickname、avatar_url、status、auth_valid_after、last_login_time、version、created_time、updated_time`；本阶段不拆分 `user_profile`，也不提前加入 `deleted_time、email、phone` 等尚无当前用例的字段，后续真实需要时再通过 Flyway 迁移增加。
  - [x] **D23 用户名数据库语义**：`username` 使用 `VARCHAR(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL`，唯一索引据此区分大小写；应用层仍执行 D02 的格式校验，数据库字符集和 Collation 作为第二层约束。该长度同时覆盖最长合法注册用户名和内部注销占位值 `#deleted#<userId>`。
  - [x] **D24 密码摘要存储格式**：`password_hash` 使用 `VARCHAR(255) NOT NULL`，采用 Spring Security 的 `{id}encodedPassword` 格式，当前写入 `{bcrypt}<哈希值>`；由 `DelegatingPasswordEncoder` 根据前缀选择验证器，为以后逐次升级密码算法保留兼容路径，不另建算法字段，也不把列限制为 BCrypt 固定长度。
  - [x] **D25 时间生成职责**：账户与认证时间统一由 Java 应用生成，注入 `Clock` 并以 UTC `Instant` 表达，不使用 MySQL 的 `CURRENT_TIMESTAMP` 自动初始化或自动更新时间。注册时 `created_time、updated_time、auth_valid_after` 必须取同一个瞬间；测试通过固定 `Clock` 精确验证时间边界。
  - [x] **D26 时间数据库表示**：所有账户与认证时间列使用 `DATETIME(3)`，数据库值统一解释为 UTC；领域层保持 `Instant`，持久化适配层显式完成 UTC 转换，并在实际配置切片把 JDBC 连接时区统一为 UTC。北京时间只在接口或日志展示边界转换，不写入权威数据。
  - [x] **D27 密码长度计量**：密码长度按 Unicode Code Point 计算为 6～25，同时要求 UTF-8 编码后不超过 72 字节，以避开 BCrypt 输入上限；禁止空白字符和控制字符，不执行 `trim`、大小写转换或 Unicode 规范化，确保注册与登录对原始密码使用完全相同的字节序列。
  - [x] **D28 其余账户字段定义**：`id` 使用有符号 `BIGINT NOT NULL AUTO_INCREMENT`；`nickname VARCHAR(32) NULL`；`avatar_url VARCHAR(500) NULL`；`status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL`；`auth_valid_after、created_time、updated_time` 为 `DATETIME(3) NOT NULL`；`last_login_time DATETIME(3) NULL`；`version INT NOT NULL`。注册时由 Java 显式写入 `NORMAL、0` 和三个相同的初始时间，不依赖数据库默认值。
  - [x] **D29 账户约束与索引**：只建立主键、`username` 唯一索引、账户状态集合 `CHECK` 和 `version >= 0` 的 `CHECK`；用户名、昵称和密码的复杂内容规则由 Java 校验。当前不建立低选择性的 `status` 单列索引，等出现真实管理查询后根据 `EXPLAIN` 决定索引。
  - [x] **D30 注册成功契约**：`POST /api/auth/register` 接收 `username、password、nickname?`，成功返回 `201 Created` 和直接 JSON `userId、username、nickname、displayName`；不返回密码摘要、状态或认证内部字段。当前注册切片不创建 Session、Refresh Token、Cookie 或 Access Token。
  - [x] **D31 并发重复注册**：先查询用户名以处理常见重复分支，同时由唯一索引保证并发条件下的最终正确性；两个请求同时通过预检查时，将后提交请求的唯一约束异常转换为与预检查相同的业务失败。
  - [x] **D32 注册测试结构**：采用分层测试——规则纯单元测试、应用用例测试、MockMvc HTTP 契约测试和现有 Docker MySQL 集成测试；MySQL 测试覆盖 Flyway、大小写敏感唯一索引、`CHECK`、自增 ID 与 UTC 时间往返。Docker 未启动时先完成不依赖数据库的测试，不引入 H2 或 Testcontainers。
  - [x] **D33 注册后自动登录编排**：后端继续保持注册与登录两个独立用例；前端收到注册成功响应后，只在内存中短暂复用本次输入的用户名和密码调用登录接口，登录响应负责返回 Access Token 并设置 Refresh Cookie，随后立即清除前端密码变量，不把密码写入持久化存储。注册接口不得绕过登录用例自行创建 Session 或签发 Token。
  - [x] **D34 注册领域建模**：`Nickname.optionalOf` 将 `null` 或 strip 后空白映射为未设置，非空昵称校验 1～32 Unicode Code Point 并拒绝控制符和换行；`RawPassword` 与 `PasswordHash` 使用隐藏 `toString()` 的普通 final class，避免敏感值被 record 自动输出。`UserAccount.register` 通过注入的 `Clock` 只读取一次时间，持久化前以 `id == null` 表示尚无自增 ID，初始化 `NORMAL、version=0、avatarUrl=null、lastLoginTime=null`，并保证 `createdTime = updatedTime = authValidAfter`。
  - [x] **D35 账户持久化适配**：使用项目既定的 MyBatis-Plus `BaseMapper` 完成当前注册切片的简单存在性查询和插入，不建立 Mapper XML、`IService` 或 `ServiceImpl`。应用层只依赖最小 `UserRepository` 端口；基础设施层使用 `UserAccountPo`、UTC Converter、`UserAccountMapper` 和 `MyBatisUserRepository`。自增 ID 由 MyBatis-Plus 回填到 PO，Repository 返回生成的 ID，不为不可变领域对象增加 setter。
  - [x] **D36 BCrypt 与注册用例**：当前只引入 `spring-security-crypto`，不提前启用完整 Security Starter。2026-07-16 在当前 Java 24 Maven 运行环境串行测得 strength 10～14 的验证中位数为 47、100、191、381、695 ms，选择测试范围内最高且低于约 1 秒目标的 strength 14，并允许通过 `EPF_BCRYPT_STRENGTH` 覆盖。使用只注册 `bcrypt` 的 `DelegatingPasswordEncoder` 保存 `{bcrypt}` 前缀；`RegisterUserService` 在事务外完成校验、查重和哈希，独立 `UserAccountRegistrationTransaction` 只包围账户 INSERT，避免 BCrypt 期间占用数据库事务。

**用户线：注册纵向切片**

- D01～D33 已完成用户 ID、用户名/昵称/密码规则、账户字段、UTC 时间、状态、索引、Flyway、注册契约与测试边界确认；实现以这些决策为准。
- 使用 Flyway 创建首个版本化 `user_account` 迁移；关键 DDL、索引和约束仍由用户主导编写并说明正确性依据。
- 只创建注册、登录和认证状态所需的 `user_account`。
- 实现用户领域模型、Repository、MyBatis 适配、密码摘要和 `POST /api/auth/register`。
- 不创建 Session、Refresh Token、Redis Key、Outbox、Topic 或用户创建事件。

#### 任务：I-01 用户注册纵向切片（进行中）

**学习目标**

- 掌握注册请求从 HTTP 边界到领域规则、应用用例、Repository 和 MySQL 的完整调用链。
- 掌握值对象校验、固定 `Clock`、BCrypt 适配、事务边界、唯一索引兜底和分层测试。
- 掌握 Flyway 版本化迁移、MySQL 表结构/索引查看，以及当前切片的 Git 小提交工作流。

**执行顺序**

- [x] **I-01.1 领域规则与注册初始状态**：用户名、昵称、注册原始密码、密码摘要包装、账户初始状态、结构化 violation 和字段级 HTTP 错误码映射均已完成，并通过目标测试。
- [x] **I-01.2 账户表与 Flyway**：已在 Docker MySQL 8.0.46 的空 `identity_db` 上执行 V1，Flyway 历史、真实 DDL、`ascii_bin`、唯一索引、两个 `CHECK`、自增 ID 与 UTC 毫秒往返均通过验证。
- [x] **I-01.3 Repository 与 MyBatis**：按 D35 使用 MyBatis-Plus `BaseMapper`；已完成最小仓储端口、PO、UTC Converter、Mapper、Repository 适配器和测试，没有创建 Mapper XML；唯一约束异常先转换为稳定业务异常，HTTP 错误契约留到注册失败分支。
- [x] **I-01.4 BCrypt 与注册用例**：已完成 BCrypt 本机基准、密码端口与适配器、受保护注册命令、`RegisterUserUseCase`、公开结果和短事务写入；注册只创建账户，不创建任何认证状态。
- [x] **I-01.5 HTTP 契约**：已实现 `POST /api/auth/register` 的 Request/Response、Controller、错误映射和 MockMvc 契约测试；D11/D12 已在失败分支编码前完成决策。
- [ ] **I-01.6 集成验收与收口**：Docker MySQL 集成测试和成功/失败路径观测已完成；还剩用户主导的 Git 状态检查、选择性暂存与提交收口。

**已完成子任务：I-01.1 领域规则与注册初始状态**

**共同决策**

- [x] 首个领域 API 从 `Username` 值对象开始；规则以 D02 为唯一依据，不在 Controller、DTO 和数据库适配层重复实现。
- [x] 首个测试只验证一个行为，后续以独立测试逐条增加边界，遵循 Red → Green → Refactor。

**你实现**

- [x] 在 `identity-service/src/test/java/com/everypicfound/identity/domain/model/user/UsernameTest.java` 编写首个测试：输入 `"  User_01  "` 时，规范化后的值为 `"User_01"`。
- [x] 运行该单测并确认失败原因是 `Username` 尚不存在，而不是测试语法、JDK 或 Maven 参数错误。
- [ ] 说明为什么 `strip()` 只处理首尾、为什么领域对象保留大小写，以及规则为什么不能只依赖前端。

**Codex 生成**

- [x] Review 首个关键测试，并补齐用户名、注册原始密码及字段级 HTTP 错误映射测试；密码使用隐藏 `toString()` 的普通 final class，避免 record 自动输出敏感原文。
- [ ] 在后续子任务生成 PO、Converter、Request/Response 和 Mapper 的机械性代码。

**验证**

- [x] RED 命令：`.\mvnw.cmd -pl identity-service -am -Dtest=UsernameTest -Dsurefire.failIfNoSpecifiedTests=false test`。2026-07-16 实际结果：父工程与 `security-contract` 成功，`identity-service` 在测试编译阶段因找不到 `Username` 失败，符合预期 RED；`-am` 同时构建其依赖模块，附加参数避免没有该测试的依赖模块误报失败。
- [x] GREEN：实现 `Username`、`Nickname`、`RawPassword`、`PasswordHash`、`UserAccount.register`、`AccountStatus`、结构化 violation、领域异常、`UserErrorCode` 与 `IdentityExceptionHandler`。验证命令：`.\mvnw.cmd -pl identity-service -am "-Dtest=UsernameTest,RawPasswordTest,NicknameTest,UserAccountTest,IdentityExceptionHandlerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`；2026-07-16 结果为 63 tests、0 failures、0 errors、0 skipped。
- [ ] 当前不运行 Docker、全量构建或仓库旧测试。

**观测与 Linux**

- [ ] I-01.1 先学习 Maven 目标测试输出；MySQL 与 Linux 命令练习留到 I-01.2 真实迁移验证时执行。

**Git**

- [x] 用户执行 `git status --short`，识别准备阶段改动与本切片新增文件。
- [x] 未自动暂存或提交；I-01 收口时由用户选择性暂存并检查 staged diff。

**复盘**

- 掌握内容：Maven 主/测试源码集边界；领域 violation 与客户端 errorCode 的分层映射；密码 Unicode Code Point、UTF-8 72 字节边界及敏感 `toString()` 防护。
- 失败与原因：首轮 RED 因 `Username` 尚不存在而失败；本批 RED 因 `RawPassword`、violation、领域异常和 HTTP 错误映射尚不存在而失败。一次验证曾被沙箱网络权限阻断，另一次因 PowerShell 未给 `-D` 参数加引号而被 Maven 误判为生命周期名称，修正运行环境和参数引号后获得真实 RED/GREEN。
- 使用命令：`.\mvnw.cmd -pl identity-service -am "-Dtest=UsernameTest,RawPasswordTest,IdentityExceptionHandlerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`。
- 待复习问题：MyBatis 插入自增 ID 后如何将持久化身份带回领域对象，以及数据库 UTC `DATETIME(3)` 与 `Instant` 的显式转换。

**已完成子任务：I-01.3 Repository 与 MyBatis-Plus 持久化适配**

**共同决策**

- [x] 简单存在性查询和插入直接使用 MyBatis-Plus `BaseMapper`，不建立 Mapper XML、`IService` 或 `ServiceImpl`。
- [x] 应用与领域层只依赖 `UserRepository`；MyBatis-Plus、PO 和查询 Wrapper 只存在于基础设施层。
- [x] `UserAccount` 保持不可变；`@TableId(type = IdType.AUTO)` 令生成 ID 回填到 PO，Repository 将 ID 返回给应用用例。

**Codex 生成**

- [x] `UserRepository`、`UserAccountPo`、`UserAccountPersistenceConverter`、`UserAccountMapper`、`MyBatisUserRepository` 与 `UsernameAlreadyExistsException`。
- [x] Converter 和 Repository 的 4 个目标测试，覆盖 UTC 毫秒转换、存在性结果、自增 ID 返回和重复键异常转换。

**验证**

- [x] RED：引入测试和 MyBatis-Plus 依赖后运行目标测试，测试编译因上述持久化生产类型尚不存在而失败，共报告 15 个“找不到符号”，失败原因符合预期。
- [x] GREEN：运行 `UserAccountPersistenceConverterTest,MyBatisUserRepositoryTest`，4 tests、0 failures、0 errors、0 skipped。
- [x] 回归：运行 I-01.1 与 I-01.3 共 7 个测试类，67 tests、0 failures、0 errors、0 skipped，构建成功。
- [x] I-01.6 已使用真实 MySQL 验证 Collation、约束、自增回填和 UTC 往返。

**复盘**

- 掌握内容：`BaseMapper` 是基础设施实现细节；PO 承接数据库形态，领域模型保持业务形态；`DATETIME(3)` 通过 UTC `LocalDateTime` 边界转换；唯一索引负责并发重复注册的最终兜底。
- 运行环境提示：当前 Maven 使用 Java 24 运行并按 Java 17 编译，Mockito 输出动态加载 Agent 的未来兼容性警告，但本批测试未失败；是否调整测试 JVM 或 Mockito Agent 作为独立构建治理问题处理，不在账户持久化切片额外引入配置。
- 已验证：真实 MySQL 迁移和插入、`CaseUser`/`caseuser` 共存、精确及并发重复失败、CHECK 约束、自增 ID 与 UTC 毫秒往返。

**已完成子任务：I-01.4 BCrypt 与注册应用用例**

**共同决策**

- [x] 只加入 `spring-security-crypto`；自定义 `DelegatingPasswordEncoder` 当前只映射 BCrypt，不添加 `noop`，生产摘要为 `{bcrypt}<BCrypt>`。
- [x] 显式串行运行 `BCryptCostBenchmark`，strength 10～14 的中位数依次为 47、100、191、381、695 ms；当前选择 strength 14，部署环境变化后重新测量。
- [x] `PasswordHasher` 同时提供哈希和匹配能力；登录切片真实需要升级摘要时再增加 `upgradeEncoding`，当前不提前实现。
- [x] BCrypt、输入校验和查重在事务外执行；独立 Spring Bean 的 `@Transactional save` 只包围账户写入，避免同类自调用导致事务代理失效。

**Codex 生成**

- [x] `PasswordHasher`、`PasswordHashProperties`、`PasswordHashConfiguration`、`BCryptPasswordHasher` 和 `PasswordHashingException`。
- [x] `RegisterUserCommand`、`RegisterUserResult`、`RegisterUserUseCase`、`RegisterUserService`、`UserAccountRegistrationTransaction` 和 UTC `Clock` Bean。
- [x] BCrypt 手工基准与 9 个目标测试，覆盖前缀、随机盐、匹配、异常脱敏、命令脱敏、成功编排、重复短路和短事务边界。

**验证**

- [x] 密码层 RED：目标测试在测试编译阶段因 12 个密码生产类型符号缺失而失败；GREEN 为 4 tests、0 failures、0 errors、0 skipped。
- [x] 注册层 RED：目标测试在测试编译阶段因 13 个注册生产类型符号缺失而失败；GREEN 为 5 tests、0 failures、0 errors、0 skipped。
- [x] 相关回归：运行 I-01.1～I-01.4 共 12 个测试类，76 tests、0 failures、0 errors、0 skipped，构建成功；手工 BCrypt 基准不进入该默认相关测试集合。
- [x] I-01.6 已在 Docker MySQL 上验证真实并发唯一约束、短事务写入和完整注册链路。

**复盘**

- 掌握内容：BCrypt 工作因子近似指数增长；随机盐使相同密码产生不同摘要；`{id}` 前缀为算法迁移提供路由；高成本 CPU 工作不应放进长数据库事务；Spring `@Transactional` 需要通过另一个 Bean 的代理调用生效。
- 敏感信息边界：`RawPassword`、`PasswordHash` 和 `RegisterUserCommand` 均隐藏 `toString()`；基准只使用固定测试密码且不打印密码或摘要。
- 已处理：I-01.5 已锁定用户名重复 HTTP 409，以及密码哈希/数据库系统异常的统一 HTTP 500 响应。

**已完成子任务：I-01.5 注册 HTTP 契约**

**共同决策**

- [x] `POST /api/auth/register` 成功返回 HTTP 201 和直接 JSON `userId、username、nickname、displayName`，不创建 Token、Session 或 Cookie。
- [x] 用户名重复返回 HTTP 409、`USER_USERNAME_ALREADY_EXISTS`、`field=username`。
- [x] 密码哈希和数据库内部故障统一返回 HTTP 500、`SYSTEM_INTERNAL_ERROR`、`field=null`；具体异常只写服务端日志，不向客户端暴露基础设施细节。
- [x] Request 不使用 Bean Validation 重复领域规则；HTTP 层只完成 JSON 与应用命令转换，规则仍由 `Username、Nickname、RawPassword` 统一执行。

**Codex 生成**

- [x] `RegisterUserRequest`、`RegisterUserResponse` 和 `AuthController`；Request 覆盖 `toString()` 隐藏原始密码。
- [x] 扩展 `IdentityExceptionHandler` 与稳定错误码映射，区分可处理的 400/409 和不可处理的统一 500。
- [x] 6 个 MockMvc 契约测试，覆盖 201 成功响应、请求脱敏、领域输入 400、用户名重复 409、哈希失败 500 和数据库失败 500。

**验证**

- [x] RED：运行 `AuthControllerTest`，测试编译因 `AuthController` 与 `RegisterUserRequest` 尚不存在而报告 4 个“找不到符号”，失败原因符合预期。
- [x] GREEN：`AuthControllerTest` 运行 6 tests、0 failures、0 errors、0 skipped。
- [x] 相关回归：运行 I-01.1～I-01.5 共 13 个测试类，82 tests、0 failures、0 errors、0 skipped，构建成功。
- [x] I-01.6 已通过加载完整 Spring 上下文的 MockMvc 测试验证真实 Controller → BCrypt → MySQL 闭环；接口层切片测试仍继续使用模拟用例保持快速和故障定位清晰。

**复盘**

- 掌握内容：Controller 只负责协议适配，用例负责业务编排，领域对象负责输入规则；HTTP 409 表示当前资源状态冲突，HTTP 500 对客户端保持统一而在服务端保留可观测差异。
- 敏感信息边界：Request 和 Command 都隐藏密码；错误响应、成功响应与异常日志均不记录密码原文或密码摘要。
- 测试观察：内部故障用例会按设计产生 ERROR 日志和堆栈，但客户端响应只含统一公开错误；Java 24 下现有 Mockito 动态 Agent 警告仍是独立构建治理问题。

**I-01.6 Docker MySQL 集成验收与 Git 收口（已完成）**

**环境与范围**

- [x] 只使用已启动的 `everypicfound-mysql`，未启动 Redis、RocketMQ、Debezium 或其他业务服务。
- [x] MySQL 版本为 8.0.46；`identity_db` 在首次测试前为空，业务账户已具备该库的完整权限；全局和会话时区均为 `+08:00`。
- [x] 集成测试通过 `EPF_TEST_MYSQL_ENABLED=true` 显式启用，URL、用户名和密码只从运行环境传入，不写入测试源码或文档；未启用时普通测试不要求 Docker。
- [x] 集成测试把 BCrypt strength 临时覆盖为 4 以缩短反馈时间，生产默认 strength 14 未改变。

**验证结果**

- [x] 首次 Spring 启动成功验证并执行 V1，创建 `flyway_schema_history` 和 `user_account`；后续启动报告 schema 位于版本 1 且无需迁移。
- [x] `SHOW CREATE TABLE` 与 `SHOW INDEX` 确认 `BIGINT AUTO_INCREMENT` 主键、`ascii_bin` 用户名、`uk_user_account_username` 唯一索引和两个 `CHECK` 均真实存在。
- [x] 8 个真实 MySQL 集成测试覆盖：Flyway 历史与结构、大小写敏感用户名、自增 ID、UTC `DATETIME(3)` 往返、精确重复、并发重复、两个 CHECK、真实 HTTP 注册 BCrypt 落库和重复 409。
- [x] 集成测试结束后固定测试账户全部清理，`user_account` 剩余测试记录数为 0；表结构与 Flyway 历史保留。
- [x] 最终相关回归运行 14 个测试类，共 90 tests、0 failures、0 errors、0 skipped，构建成功。

**真实启动发现与修正**

- [x] 首轮 Flyway 已成功，但 Spring 创建 `MyBatisUserRepository` 时因该 `@Repository` 是 `final class` 而无法生成默认 CGLIB 异常转换代理；移除类级 `final` 后上下文启动成功。Mockito 直接构造的 Repository 单测不会经过 Spring 代理，因此无法发现此类容器集成问题。
- [x] MySQL CHECK 违反返回 SQLState `HY000`、错误码 3819；Spring JDBC 将其包装为 `UncategorizedSQLException`，而不是更具体的 `DataIntegrityViolationException`。测试因此断言稳定的 `DataAccessException` 边界和约束名称，不绑定驱动翻译细节。

**命令练习与 Git 收口**

- [x] 已使用 `docker ps` 识别容器健康状态和端口映射，使用 `docker exec ... mysql` 查看数据库、授权、时区、DDL、索引和 Flyway 历史。
- [x] 用户运行 `git status --short` 区分原有工作区改动与 I-01 文件，再使用 `git diff -- <path>` 审查已跟踪文件。
- [x] 只选择性暂存 I-01 相关文件，Codex 运行 `git diff --cached --check`、`git diff --cached --stat` 和 staged 文件清单完成审核，并创建独立功能提交。

  **认证线：Token 基础能力**

  - 已确认 JWT/Session/Refresh TTL、Clock Skew 与 RSA 密钥基线；开始该切片时再确认 `kid`/密钥轮换/JWK Set、Refresh Token Pepper、滑动语义、Cookie 与 CSRF/Origin 策略。
  - 只实现已经确认的 JWT Claim 构造与 RS256 签发/验签；JWK Set、Refresh Token 随机值与摘要在对应决策完成后再实现。
  - 此阶段只准备开发环境安全材料和配置入口，不建立认证业务表。

  ### 阶段 2：用户域与认证域首次汇合——登录

  - 在编码前一次性确认 `user_session` 和 `user_refresh_token` 的完整轮换信息结构，再创建相应表，避免随后刷新功能反复改表。
  - Auth 通过最小用户认证读取端口获得 `userId、passwordHash、status、authValidAfter`。
  - 首次创建登录保护 Redis Key 和 `record_login_failure.lua`。
  - 在同一 MySQL 事务中创建 Session、Refresh Token并更新 `last_login_time`；提交后才签发 Access Token。
  - 实现 `POST /api/auth/login`，JWT 必须包含 `sub、sid、jti、scope、auth_time、iat、exp`。
  - 至此形成“注册产生 userId → 登录绑定 Session → Token 标识当前用户”的第一条闭环。

  ### 阶段 3：尽早建立完整认证主干

  - 为 `identity-service` 建立公开接口与受保护接口安全链，保证 logout、Session、用户接口能从 `SecurityContext` 读取身份。
  - 首次实现认证状态查询时，再创建 `auth:user:{userId}:state` 和单调回填 Lua。
  - 实现内部用户认证状态接口及 JWK Set 接口；内部接口的信任机制必须在编码前从“仅内网隔离、共享内部凭证、mTLS/内部签名”中确认，不能由实现者自行决定。
  - Gateway 实现标准 JWT 校验、用户/Session 屏障检查、`sid deny` 检查、用户状态回源和 `auth_time` 比较。
  - `jti deny` 保持延期，因为当前没有只撤销单张 Access Token 的用例。
  - Media 实现 Resource Server 二次验签、`CurrentUserProvider` 和 Scope 授权，不改变现有 SearchPipeline。
  - 形成首个跨服务闭环：登录 → Gateway 动态认证 → Media 二次验签 → 搜索/图片接口。

  ### 阶段 4：认证与用户常规能力交叉推进

  - 认证线实现 Refresh Token 正常轮换，使用 MySQL 条件更新保证并发刷新只有一个成功，不引入 Redis Lua 或 MQ。
  - 用户线实现 `GET /api/users/me` 和昵称修改；首版直接访问 MySQL。
  - 实现 Session 查询并标记当前 Session，返回脱敏设备信息。
  - 当前没有资料缓存和下游消费者，因此首版不创建资料缓存、`USER_PROFILE_CHANGED` Outbox 或 Topic；同时把设计文档中的缓存方案明确标记为后续可选优化。

  ### 阶段 5：Session 撤销与 Outbox/RocketMQ 决策点

  在 Refresh Token 重放或当前设备退出首次需要可靠补偿时，暂停编码并确认 Outbox Relay：

  - **方案 A，文档当前基线**：应用定时扫描 `PENDING` Outbox，通过 RocketMQ 客户端发布；表需要状态、重试次数和发布时间等字段。
  - **方案 B，CDC 方案**：Debezium 监听 MySQL Binlog 并发布 RocketMQ；Outbox 使用追加式事件结构，不混入应用轮询状态字段。
  - 在选型前补充两篇设计文档，说明 Debezium、CDC、MySQL Binlog、RocketMQ Sink、重复投递与幂等消费机制，并记录最终选择。
  - RocketMQ 固定为消息队列；具体客户端、Topic、Consumer Group、重试/DLQ、消息 Key 和顺序语义在该决策中一并确认。
  - 选型后才创建 `outbox_event`、首个认证状态 Topic、消费者和必要配置，不沿用旧计划中的 Kafka 名称或 Kafka DLT 语义。
  - 创建 Session barrier、`sid deny`、Session 撤销发布 Lua 和按 `operationId` 安全释放 Lua。
  - 实现 Refresh Token 重放处理与 `POST /api/auth/logout`；Redis 即时发布失败时由 Outbox → RocketMQ → 幂等 Lua 补偿。

  ### 阶段 6：用户安全操作与认证撤销再次汇合

  - 实现修改密码：校验旧密码、更新摘要、推进 `auth_valid_after`、撤销全部 Session/Refresh Token、发布用户认证状态和各 Session deny。
  - 实现账号逻辑注销：状态改为 `DELETED`，推进认证分界点并撤销登录状态。
  - 用户级事件与 Session 事件继续复用已确认的认证状态消息通道。
  - `USER_DELETED` 生命周期消息只有出现 Media 等真实消费者时才建立独立消息契约和 Topic；不得创建无人消费的 Topic。
  - 本阶段复用阶段 5 的 Outbox、RocketMQ、屏障和 Lua，不重复建设另一套一致性机制。

  ### 阶段 7：接口、安全与可观测性收口

  首轮对外接口：

  ```
  POST   /api/auth/register
  POST   /api/auth/login
  POST   /api/auth/refresh
  POST   /api/auth/logout
  GET    /api/auth/sessions
  GET    /api/users/me
  PATCH  /api/users/me/profile
  POST   /api/users/me/password
  DELETE /api/users/me
  ```

  内部接口：

  ```
  GET /internal/auth/users/{userId}/state
  GET /internal/.well-known/jwks.json
  ```

  - 统一 Request、Command、Domain、PO、Result、Response 边界和错误码。
  - 完成 Cookie 清理、CSRF/Origin、防敏感日志、401/403/429、Fail Closed 和必要指标。
  - Gateway 限流规则和具体实现参数在认证链路稳定后单独确认，不提前固化。
  - 指定设备下线、退出全部设备、管理员禁用/启用、`jti deny`、资料缓存与用户生命周期消费列入后续迭代。

  ## 四、测试与验收

  - 不以现有过时测试作为本轮规划和准备阶段的阻塞条件。
  - 每个切片同步建立新测试：领域单元测试、接口测试，以及首次使用 MySQL/Redis/RocketMQ 时才加入对应集成测试环境。
  - 重点覆盖：并发注册、登录失败锁定、事务失败不签 Token、JWT 声明、并发刷新、缓存旧值不能覆盖新值、重放撤销、重复退出、RocketMQ 重复消费幂等和 Redis 故障补偿。
  - Media 验收必须包含绕过 Gateway 直接访问时的本地验签。
  - 最终端到端验收链路：

  ```
  注册 → 登录 → Gateway 标准及动态校验
  → Media 二次验签 → 搜索
  → Refresh Token 轮换
  → 当前设备退出
  → 原 Session Access Token 被拒绝
  → Redis 发布失败时由 Outbox + RocketMQ 完成补偿
  ```

  - 全部新功能完成后再单独盘点旧测试：仍有价值的迁移到新模块，已失效的明确替换或删除；不得为了让旧测试通过而扭曲新架构。

  ## 五、明确假设与强制决策门

  - 首轮默认采用“核心自助闭环”，暂缓指定设备下线、退出全部设备和管理员禁用/启用。
  - 消息队列固定为 RocketMQ；Kafka 相关内容从新计划中移除。
  - Profile 首版不启用缓存或资料变更事件；`jti deny` 不实现。
- Flyway、`BIGINT AUTO_INCREMENT` 与 BCrypt 已在阶段 1 决策中锁定；内部服务信任、Cookie/CSRF 和 Outbox Relay 仍属文档未锁定事项，必须在首次编码前向用户给出候选方案并确认。
  - Debezium/CDC 不因当前 Compose 已存在而自动成为选定方案；选择前必须先补充和修订两篇技术设计文档。
