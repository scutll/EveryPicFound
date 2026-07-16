# Spring Security JOSE 与 RSA JWT 签发验签

## 摘要

本文说明如何使用 Spring Security JOSE 建立一条最小而完整的 RSA JWT 链路：生成和保存密钥、加载 PKCS#8 私钥与 X.509 公钥、配置 Token TTL、签发 JWT、组合校验器、测试时间边界以及排查常见错误。

JWT 的 Base64URL 编码不是加密。任何拿到 Token 的人都可以读取 Header 和 Payload；密码、密钥、身份证号等敏感信息不得放进 Claim。RSA 私钥只由签发方持有，验证方只需要公钥。

## 一、签发与验签流程

```mermaid
flowchart LR
    A["业务用例提供主体、会话和权限"] --> B["签发器生成 iat、nbf、exp、jti"]
    B --> C["Header 与 Payload 分别 Base64URL 编码"]
    C --> D["私钥对 header.payload 执行 RS256 签名"]
    D --> E["JWT: header.payload.signature"]
    E --> F["验证方使用公钥验证签名"]
    F --> G["校验 iss、aud、exp、nbf 和必要 Claim"]
    G --> H["通过后建立可信认证上下文"]
```

签名解决的是完整性和来源问题：Payload 被修改后，原签名无法再通过公钥验证。它不负责隐藏 Payload，也不自动判断 Token 是否适用于当前系统；签名通过后仍必须检查 Issuer、Audience 和时间等约束。

## 二、RSA 密钥准备

### 2.1 使用 OpenSSL 生成

在仓库外或被明确忽略的安全目录执行：

```bash
openssl genpkey -algorithm RSA \
  -pkeyopt rsa_keygen_bits:2048 \
  -out jwt-private.pem

openssl pkey \
  -in jwt-private.pem \
  -pubout \
  -out jwt-public.pem
```

应得到：

- 私钥：`-----BEGIN PRIVATE KEY-----`，Java 使用 `PKCS8EncodedKeySpec` 解析。
- 公钥：`-----BEGIN PUBLIC KEY-----`，Java 使用 `X509EncodedKeySpec` 解析。

不要把 `-----BEGIN RSA PRIVATE KEY-----` 的 PKCS#1 文件直接交给只支持 PKCS#8 的加载器。已有 PKCS#1 私钥可以显式转换：

```bash
openssl pkcs8 -topk8 -nocrypt \
  -in old-rsa-private.pem \
  -out jwt-private.pem
```

### 2.2 保存边界

- 私钥只能由 Token 签发服务读取，不能进入 Git、镜像层、日志或接口响应。
- 公钥可以分发给验证服务，但仍应通过可审计的配置渠道管理，防止被错误替换。
- Linux 本地私钥至少限制为当前用户可读：`chmod 600 jwt-private.pem`。
- 测试应动态生成 KeyPair 并写入临时目录，不要提交“测试私钥”。
- 生产环境可使用外部文件挂载或专用密钥管理能力；采用哪种方案属于部署架构决策，不能仅由 JWT 代码自行决定。

## 三、依赖和配置属性

最小依赖是 Spring Security JOSE，不需要为了签发 Token 提前启用完整 Resource Server：

```xml
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-oauth2-jose</artifactId>
</dependency>
```

配置应允许从外部覆盖，且只保存密钥位置，不保存 PEM 正文：

```yaml
security:
  jwt:
    issuer: ${JWT_ISSUER}
    audience: ${JWT_AUDIENCE}
    access-token-ttl: ${JWT_ACCESS_TOKEN_TTL:15m}
    clock-skew: ${JWT_CLOCK_SKEW:30s}
    private-key-location: ${JWT_PRIVATE_KEY_LOCATION}
    public-key-location: ${JWT_PUBLIC_KEY_LOCATION}
```

```java
@ConfigurationProperties("security.jwt")
public record JwtProperties(
        String issuer,
        String audience,
        Duration accessTokenTtl,
        Duration clockSkew,
        Resource privateKeyLocation,
        Resource publicKeyLocation) {
}
```

启动时应快速失败：Issuer/Audience 不能为空，TTL 必须大于零，Clock Skew 不能为负数，密钥资源必须存在且格式正确。

## 四、加载并核对 PEM 密钥

加载过程分为四步：

1. 删除 PEM 的 BEGIN/END 行和空白字符，再执行 Base64 解码。
2. 私钥使用 `KeyFactory.generatePrivate(new PKCS8EncodedKeySpec(bytes))`。
3. 公钥使用 `KeyFactory.generatePublic(new X509EncodedKeySpec(bytes))`。
4. 校验两者都是 RSA、模数至少 2048 位，并确认它们属于同一密钥对。

公私钥匹配可以使用内部探针验证：私钥通过 `SHA256withRSA` 签名一小段固定数据，再由公钥验证。错误信息只能说明资源不可读、格式错误、强度不足或密钥不匹配，不能拼接 PEM、Base64、`Key#toString()` 或私钥参数。

## 五、配置 Encoder 和 Decoder

### 5.1 Encoder 持有公私钥

Spring Security 6.5 的 `NimbusJwtEncoder` 接收 `JWKSource`：

```java
RSAKey rsaKey = new RSAKey.Builder(publicKey)
        .privateKey(privateKey)
        .build();

ImmutableJWKSet<SecurityContext> source =
        new ImmutableJWKSet<>(new JWKSet(rsaKey));

JwtEncoder encoder = new NimbusJwtEncoder(source);
```

单密钥阶段可以不写 `kid`。出现多密钥轮换和公钥集合后，再设计 `kid` 的生成、选择和旧公钥保留规则。

### 5.2 Decoder 只持有公钥

```java
NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(publicKey)
        .signatureAlgorithm(SignatureAlgorithm.RS256)
        .build();
```

显式限定 RS256，避免验证端接受非预期算法。Decoder 只需要公钥，不应获得私钥。

## 六、签发 Access Token

### 6.1 时间和 Header

JWT NumericDate 使用 Unix epoch 秒。为了避免毫秒精度在不同库之间产生差异，可以在签发时统一到秒：

```java
Instant issuedAt = Instant.ofEpochSecond(clock.instant().getEpochSecond());
Instant expiresAt = issuedAt.plus(properties.accessTokenTtl());

JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256)
        .type("JWT")
        .build();
```

### 6.2 Payload

```java
JwtClaimsSet claims = JwtClaimsSet.builder()
        .issuer(properties.issuer())
        .subject(subject)
        .audience(List.of(properties.audience()))
        .issuedAt(issuedAt)
        .notBefore(issuedAt)
        .expiresAt(expiresAt)
        .id(UUID.randomUUID().toString())
        .claim("sid", sessionId)
        .claim("scope", normalizedScope)
        .claim("auth_time", authTime.getEpochSecond())
        .build();

String token = encoder.encode(
        JwtEncoderParameters.from(header, claims)).getTokenValue();
```

常用 Claim 的职责：

| Claim | 含义 |
| --- | --- |
| `iss` | 谁签发了 Token |
| `aud` | Token 允许被哪个系统消费 |
| `sub` | Token 对应的主体 |
| `jti` | 当前 Token 的唯一标识 |
| `iat` | Token 的签发时间 |
| `nbf` | 在此时间之前不可接受 |
| `exp` | 到此时间后不可接受 |
| `sid` | 关联的登录会话标识 |
| `scope` | 授予的权限范围，通常为稳定排序的空格分隔字符串 |
| `auth_time` | 用户完成本次认证的时间，不一定等于 Token 刷新时间 |

`auth_time` 是自定义时间 Claim，应主动写成 epoch 秒。直接把 `Instant` 当普通自定义对象交给底层 JSON 序列化器，可能触发 Java 模块反射错误。

## 七、组合验证规则

仅验证签名是不够的。一个典型的验证链至少包括：

```java
JwtTimestampValidator timestamp =
        new JwtTimestampValidator(properties.clockSkew());
timestamp.setClock(clock);

decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(List.of(
        timestamp,
        new JwtIssuerValidator(properties.issuer()),
        new JwtAudienceValidator(properties.audience()),
        new JwtRequiredClaimsValidator())));
```

- Timestamp Validator 检查 `exp` 和 `nbf`，Clock Skew 用于容忍不同服务器之间的小幅时钟误差。
- Issuer Validator 防止接受其他签发系统的 Token。
- Audience Validator 防止把签给其他 API 的 Token 用在当前 API。
- Required Claims Validator 保证协议要求的 Claim 存在，并检查关键字符串不是空值。

### 7.1 `JwtIssuedAtValidator` 的版本语义陷阱

Spring Security 6.5 的 `JwtIssuedAtValidator(true)` 不只是要求 `iat` 存在；它还要求 `iat` 位于“当前时间 ± Clock Skew”范围内。若 Access Token TTL 是数分钟，它可能在 Clock Skew 过后因为 `iat claim is invalid` 被错误拒绝。

因此不要看到类名就直接加入校验链。先确认当前依赖版本的真实语义，并通过固定 Clock 测试验证。普通 Access Token 可以由必需 Claim 校验器保证 `iat` 存在，由受信签发器生成 `iat`，再由 Timestamp Validator 负责 `nbf/exp`。

## 八、TTL 和 Clock Skew

TTL 决定 Token 暴露后的最大有效窗口：TTL 越短，泄露影响越小，但刷新频率越高。具体数值应结合会话和刷新机制决定，而不是从示例配置直接复制。

Clock Skew 不是延长业务 TTL，而是容忍服务器时钟的微小差异。例如偏移为 30 秒时，应使用固定 Clock 测试：

- `exp` 已过去 29 秒仍可接受，过去 31 秒应拒绝。
- `nbf` 还有 29 秒可以接受，还有 31 秒应拒绝。

不要用 `sleep()` 测时间边界；注入 `Clock` 才能得到快速、稳定、可重复的测试。

## 九、测试矩阵

建议至少覆盖：

- 请求字段校验、Scope 去重排序及对象 `toString()` 脱敏。
- 动态生成 2048 位 RSA KeyPair，写入 JUnit 临时目录。
- 正确 PKCS#8/X.509、错误 PEM 标签、损坏 Base64、非 RSA、弱密钥和不匹配密钥。
- Header 的 `alg/typ`、全部 Claim、NumericDate、TTL 和每次不同的 UUID `jti`。
- 正常验签、篡改任意 JWT 段、错误公钥、错误 Issuer/Audience、过期和尚未生效。
- Clock Skew 内外边界。
- Encoder 失败时转换为统一内部异常，异常和日志不包含 Token、Session 或密钥正文。

解析测试 JWT 时只在本地读取前两段：

```text
Base64URL(header) . Base64URL(payload) . Base64URL(signature)
```

不要把真实 Token 粘贴到在线 JWT 网站，也不要在测试输出中打印完整 Token 或 Signature。

## 十、常见失败与排查

| 现象 | 常见原因 | 检查方向 |
| --- | --- | --- |
| 私钥无法解析 | 把 PKCS#1 当成 PKCS#8 | 检查 PEM Header，必要时使用 `openssl pkcs8` 转换 |
| 公钥无法解析 | 不是 X.509 SubjectPublicKeyInfo | 使用 `openssl pkey -pubout` 重新导出 |
| `Invalid signature` | 公私钥不匹配、Token 被修改 | 先用内部探针验证密钥对，再检查 Token 传输 |
| `Jwt expired` | Token 过期或服务器时间错误 | 检查 `exp`、UTC 时钟和 Clock Skew |
| `Jwt used before` | `nbf` 尚未到达 | 检查 NumericDate 单位是否误用毫秒 |
| `iat claim is invalid` | 错用 `JwtIssuedAtValidator` 或时钟异常 | 核对当前 Spring Security 版本语义和固定 Clock 测试 |
| 启动时读取密钥失败 | 路径、权限或挂载错误 | 检查 Resource URI、文件权限和进程工作目录 |

## 关联实现

- 项目内模块决策：[`../modules/EveryPicFound_用户与认证模块设计.md`](../modules/EveryPicFound_用户与认证模块设计.md)
- 项目内认证技术设计：[`../modules/基于JWT的登录认证与会话管理技术设计文档.md`](../modules/基于JWT的登录认证与会话管理技术设计文档.md)
- 项目内编码计划与验证记录：[`../modules/EveryPicFound 用户与认证模块编码计划.md`](../modules/EveryPicFound%20用户与认证模块编码计划.md)
- 生产签发器：`identity-service/.../jwt/adapter/SpringJoseAccessTokenIssuer.java`
- 编解码配置：`identity-service/.../jwt/config/JwtConfiguration.java`
- PEM 加载器：`identity-service/.../jwt/key/PemRsaKeyLoader.java`
