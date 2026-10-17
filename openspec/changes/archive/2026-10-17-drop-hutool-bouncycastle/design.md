## Context

2026-10-15 的 Linux arm64 Native 产物：未压缩 187.6 MiB，UPX `--best --lzma` 后 42.4 MiB。镜像统计里机器码 72.6 MiB、堆 114.6 MiB、资源 20.1 MiB。体积口径见 `docs/07-技术决策/ADR-004-Native交付分两道门并接受约38MiB.md`：不靠裁 Excel 图表、Git、双库或内嵌 SPA 来换体积。

主代码里 Hutool 只出现在三处，都在 Git Webhook：

- `GitWebhookSecretCipher`：`DigestUtil.sha256`、`SecureUtil.aes`、`RandomUtil.randomString(32)`
- `GitWebhookSigning`：`HMac` + `HmacAlgorithm.HmacSHA256`
- `GitWebhookDeliveryService`：`HttpRequest.post`，超时取自配置，不跟随重定向

`bcprov-jdk18on` 只由 `cat2bug-platform-system/pom.xml` 显式引入。注释写明是因为 `hutool-all` 把 BouncyCastle 标为 optional，Native 分析 `SmUtil` / `SecureUtil` 时必须能链接。业务代码没有 `org.bouncycastle` 引用，也没有国密调用。Quarkus BOM 把解析版本抬到 1.86，声明版本是 1.78.1。

## Goals / Non-Goals

**Goals:**

- Webhook 的 secret 存储、HMAC-SHA256 签名、出站 POST 行为与现在一致。
- 运行时 classpath 不再出现 `hutool-all` 与 `bcprov`。
- Native 构建参数不再引用 Hutool 类。

**Non-Goals:**

- 不改 Webhook 的 HTTP 头、事件名、payload 或 secret 明文只展示一次的契约（`openspec/specs/git-repo-webhooks/spec.md`）。
- 不迁移数据库里已有密文。
- 不在这次换 OkHttp、fastjson2、Lucene，也不拆 H2/MySQL、不把 SPA 移出二进制。

## Decisions

### 用 JDK 替换这三处，而不是保留 hutool-crypto 小模块

Hutool 没有单独打进工程的 crypto/http 模块；现在依赖的是 `hutool-all`（class 约 5.0 MiB）。三处调用都能用 JDK 17 写完：

- AES：`Cipher.getInstance("AES/ECB/PKCS5Padding")`，密钥 `SHA-256(secretKey)` 的前 16 字节，密文 Base64，明文 UTF-8。这与 Hutool `SecureUtil.aes(byte[])` 的默认模式一致。
- HMAC：`Mac.getInstance("HmacSHA256")`，摘要小写十六进制，头值仍是 `sha256=` + digest。
- HTTP：`java.net.http.HttpClient`，`followRedirects(NEVER)`，超时用现有 `httpTimeoutMs`。
- 新 secret：`SecureRandom` 生成 32 位小写字母和数字。只影响新生成的 secret，不影响已存密文。

备选是继续留 `hutool-all` 只为这三处。否决：BouncyCastle 会留下来，class 合计约 17 MiB 仍在 classpath 上。

### 不新增 ADR

这是依赖替换，不改变 ADR-004 的门禁和「不裁功能」决定。

## Risks / Trade-offs

- [AES 参数和 Hutool 不一致，旧 secret 解不开] → 实现前用现有密文样本对照 Hutool 输出；单测覆盖加密后再解密，以及一段已知 Hutool 密文。
- [HTTP 客户端对超时、非 2xx、连接失败的归类和现在不同] → `executePost` 的成功条件保持 200–299；异常仍记入 delivery 的错误信息，不跟随重定向。
- [UPX 后实际少不了几 MB] → 本次不把体积数字写进门禁。重新打 Native 后把前后字节数记在变更说明里。

## Migration Plan

1. 替换三个类并补兼容测试。
2. 删除两个依赖和 Native 里的 Hutool 初始化项。
3. `mvn -pl cat2bug-platform-admin -am dependency:tree` 确认没有 `hutool` 与 `bouncycastle`。
4. 回滚就是恢复依赖和这三个类，已存密文不需要数据迁移。

## Open Questions

无。

## 相关长期文档

- `docs/07-技术决策/ADR-004-Native交付分两道门并接受约38MiB.md`
- `openspec/specs/git-repo-webhooks/spec.md`
- `openspec/specs/quarkus-native-slim-binary/spec.md`

拟新增 ADR：无。
