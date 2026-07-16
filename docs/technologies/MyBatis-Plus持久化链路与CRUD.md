# MyBatis-Plus 持久化链路与 CRUD

## 1. 分层关系

业务代码调用 Repository 接口，不直接调用 MyBatis-Plus Mapper：

```text
Controller
  → Application Service
  → Repository 接口
  → Repository 实现
  → BaseMapper<PO>
  → MySQL
```

各对象只表达自己所在边界的信息：

| 对象 | 用途 | 是否依赖数据库结构 |
| --- | --- | --- |
| Request / DTO | 接收请求或在应用边界传递数据 | 否 |
| User 等领域对象 | 表达业务状态和业务规则 | 否 |
| PO | 映射数据库表、字段和乐观锁版本 | 是 |
| Mapper | 使用 MyBatis-Plus 操作 PO 对应的表 | 是 |
| Repository | 向业务层提供业务语义接口，内部转换领域对象与 PO | 接口否，实现是 |

因此不能把 PO 直接作为 Controller 响应，也不能让 Application Service 接收 `BaseMapper` 或 `Wrapper`。

## 2. 一套完整的组织方式

先定义业务真正需要的 Repository 接口：

```java
public interface UserRepository {
    boolean existsByUsername(Username username);
    long save(User user);
    Optional<User> findById(long userId);
}
```

PO 负责表映射。数据库使用自增主键时，`IdType.AUTO` 会在插入后把生成的 ID 回填到 PO：

```java
@TableName("user_account")
public class UserPo {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    @Version
    private Integer version;

    // getter / setter
}
```

Mapper 不承载业务流程，只声明对这张表的 MyBatis-Plus 操作入口：

```java
@Mapper
public interface UserMapper extends BaseMapper<UserPo> {
}
```

Repository 实现负责转换、调用 Mapper、检查结果和翻译异常：

```java
@Repository
public class MyBatisUserRepository implements UserRepository {
    private final UserMapper mapper;
    private final UserConverter converter;

    @Override
    public long save(User user) {
        UserPo po = converter.toPo(user);
        int rows = mapper.insert(po);
        if (rows != 1 || po.getId() == null) {
            throw new IllegalStateException("user insert failed");
        }
        return po.getId();
    }
}
```

Converter 的方向通常是：

```text
写入：Domain / Command → PO
读取：PO → Domain / DTO
```

字段含义不同时必须显式转换，例如领域使用 `Instant`，而 MySQL `DATETIME` 对应的 PO 使用 `LocalDateTime`。

## 3. 常用 CRUD

`BaseMapper<T>` 已提供常见操作：

```java
// Create：返回影响行数；自增 ID 回填到 po.id
int inserted = mapper.insert(po);

// Read
UserPo byId = mapper.selectById(userId);
List<UserPo> users = mapper.selectList(queryWrapper);

// Update：po 必须携带主键
int updated = mapper.updateById(po);

// Delete
int deleted = mapper.deleteById(userId);
```

Repository 应检查写操作影响行数。`0` 通常表示目标不存在、条件不满足或发生并发冲突，不能无条件当作成功。

## 4. Wrapper 查询和条件更新

优先使用 Lambda Wrapper，字段重命名时可以由编译器发现问题：

```java
LambdaQueryWrapper<UserPo> query = Wrappers.<UserPo>lambdaQuery()
        .eq(UserPo::getUsername, username)
        .eq(UserPo::getStatus, "NORMAL");

UserPo po = mapper.selectOne(query);
```

动态条件使用方法自带的 `condition` 参数：

```java
Wrappers.<UserPo>lambdaQuery()
        .eq(username != null, UserPo::getUsername, username)
        .ge(createdAfter != null, UserPo::getCreatedTime, createdAfter);
```

条件更新需要同时写清楚“修改什么”和“允许修改哪一行”：

```java
LambdaUpdateWrapper<UserPo> update = Wrappers.<UserPo>lambdaUpdate()
        .eq(UserPo::getId, userId)
        .eq(UserPo::getStatus, "NORMAL")
        .set(UserPo::getNickname, nickname);

int rows = mapper.update(null, update);
```

不要接收前端传入的 SQL 字段名或 SQL 片段，也不要把 Wrapper 作为 Controller、RPC 或 Repository 接口参数。Wrapper 是持久化实现细节。

## 5. 乐观锁

乐观锁用于避免两个请求基于同一个旧版本互相覆盖。PO 的版本字段添加 `@Version`，同时注册插件：

```java
@Bean
MybatisPlusInterceptor mybatisPlusInterceptor() {
    MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
    interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
    return interceptor;
}
```

读取到 `version = 3` 的 PO 后调用：

```java
int rows = mapper.updateById(po);
if (rows != 1) {
    throw new ConcurrentUpdateException();
}
```

MyBatis-Plus 会生成类似以下条件，并在成功时把整数版本递增后回写到 PO：

```sql
UPDATE user_account
SET nickname = ?, version = 4
WHERE id = ? AND version = 3;
```

如果其他事务已把版本改为 4，本次更新影响行数为 0。业务层需要决定是提示冲突、重新读取后重试，还是结束当前操作。使用 `update(entity, wrapper)` 时不要复用同一个 Wrapper。

## 6. Repository 与 Spring 代理约束

`@Repository` 除了表达组件语义，还可能由 Spring 的持久化异常转换机制创建代理。Spring Boot 默认偏向 CGLIB 类代理；CGLIB 通过生成目标类的子类工作，因此被代理的 Repository 类不能声明为 `final`，需要代理的方法也不能是 `final`。

```java
@Repository
public class MyBatisUserRepository implements UserRepository {
    // 可以由 CGLIB 创建子类代理
}
```

如果写成 `public final class`，普通单元测试直接 `new` 对象仍可能通过，但完整 Spring 上下文启动时会在 Bean 创建阶段失败。不要为了迁就单个类而随意全局切换 `spring.aop.proxy-target-class`；先判断该 Bean 是否确实需要事务、缓存、重试或异常转换等代理能力，再选择类代理或接口代理。至少保留一项加载真实 Spring 上下文的集成测试，才能覆盖这一类代理问题。

## 7. 推荐编写顺序

1. 根据用例确定表字段、唯一约束和并发规则。
2. 定义不依赖 MyBatis-Plus 的 Domain/User 和 Repository 接口。
3. 创建与表结构一致的 PO，配置 `@TableName`、`@TableId`，需要时配置 `@Version`。
4. 创建 `Mapper extends BaseMapper<PO>`。
5. 创建 Domain/DTO 与 PO 的 Converter。
6. 编写 Repository 实现，用 Mapper 完成 CRUD，并检查影响行数和转换基础设施异常。
7. Application Service 通过 Repository 编排事务；Controller 不访问 Mapper。
8. 单元测试 Converter 和 Repository 分支，再用真实数据库验证 SQL、约束、主键回填和乐观锁冲突。

## 关联资料

- [MyBatis-Plus 持久层接口](https://baomidou.com/guides/data-interface/)
- [MyBatis-Plus 条件构造器](https://baomidou.com/guides/wrapper/)
- [MyBatis-Plus 注解配置](https://baomidou.com/reference/annotation/)
- [MyBatis-Plus 乐观锁插件](https://baomidou.com/plugins/optimistic-locker/)
- [EveryPicFound 用户与认证模块设计](../modules/EveryPicFound_用户与认证模块设计.md)
