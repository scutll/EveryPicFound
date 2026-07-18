# 通用技术分析

本目录记录可以脱离 EveryPicFound 独立阅读和复用的技术知识，例如 RocketMQ、Redis、MySQL、Spring Security、JWT、测试、Linux 与 Git。

## 现有专题

- [MyBatis-Plus 持久化链路与 CRUD](MyBatis-Plus持久化链路与CRUD.md)：Repository、PO、Mapper 的组织方式，以及 CRUD、Wrapper 和乐观锁的基本写法。
- [Spring Cloud Gateway 路由与 JWT 权限校验](Spring-Cloud-Gateway路由与JWT权限校验.md)：Gateway 路由、JWT Resource Server、scope 权限映射、下游服务二次验权与访问流程。

## 推荐内容边界

- 工具解决的问题、适用与不适用场景。
- 核心概念、组件职责、架构和数据流。
- 通用配置、API、编码方式和参数选择原理。
- 测试、观测、运维、Linux 命令和排障方法。
- 常见错误、安全风险和版本差异。

## 与业务文档的关系

- 本目录说明“技术通常如何工作和使用”。
- [`../modules/`](../modules/) 说明“EveryPicFound 在具体模块中如何使用”。
- 不在本目录记录项目专用 Topic、Consumer Group、Redis Key、Token 有效期或业务消息字段。
- 一个技术点首次只影响单个模块时，可以先记入模块工作记录；出现长期复用价值后再提炼到这里。
