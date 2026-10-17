## Why

Native 安装包 UPX 后仍约 42 MiB。Hutool 只服务 Git Webhook 的三处调用，却把整包 `hutool-all` 和可选依赖 BouncyCastle 带进运行时。这是不改功能、不违反 [[ADR-004-Native交付分两道门并接受约38MiB]]「禁止以体积裁功能」的替换。

## What Changes

- Git Webhook 的 AES 密钥存储、HMAC-SHA256 签名、出站 HTTP 改为 JDK 标准库实现。
- 已入库的 Webhook secret 密文必须仍能用同一算法解开（`AES/ECB/PKCS5Padding`，密钥为 SHA-256 前 16 字节，密文 Base64）。
- 从 `cat2bug-platform-system` 去掉 `hutool-all` 与 `bcprov-jdk18on`。
- 从 Native 构建参数和 `NativeRuntimeInitFeature` 去掉 Hutool 相关的运行期初始化项（`SmUtil`、`DefaultSSLInfo`、`HttpGlobalConfig`、`NanoId`）。
- 不在本次改 OkHttp、fastjson2、Lucene、前端资源或双数据库驱动。

## Capabilities

### New Capabilities

- 无。

### Modified Capabilities

- `quarkus-native-slim-binary`：运行时 classpath 不再包含 Hutool 与 BouncyCastle；Webhook 的签名与 secret 存储行为保持现有契约。

## Impact

- 代码：`GitWebhookSecretCipher`、`GitWebhookSigning`、`GitWebhookDeliveryService`、`NativeRuntimeInitFeature`、`application-native.properties`、`cat2bug-platform-admin/pom.xml` 的 native profile、`cat2bug-platform-system/pom.xml`、根 `pom.xml` 的 Hutool 依赖管理。
- 依赖：去掉 `cn.hutool:hutool-all`、`org.bouncycastle:bcprov-jdk18on`。BouncyCastle 目前只有这一条依赖路径。
- 数据：不迁移已加密的 webhook secret。
- 体积：classpath 上 class 约少 17 MiB（BouncyCastle 11.6 + Hutool 5.0）。能进镜像的是其中可达部分，UPX 后的节省需重新打 Native 才能计量。
