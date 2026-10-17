## 1. Webhook 改用 JDK

- [x] 1.1 `GitWebhookSecretCipher` 改为 `MessageDigest` + `Cipher`（`AES/ECB/PKCS5Padding`，SHA-256 前 16 字节，Base64），新 secret 用 `SecureRandom` 生成 32 位小写字母和数字
- [x] 1.2 `GitWebhookSigning` 改为 `javax.crypto.Mac` 的 `HmacSHA256`，头值仍为 `sha256=` 加小写十六进制
- [x] 1.3 `GitWebhookDeliveryService` 改为 `java.net.http.HttpClient`，不跟随重定向，超时仍用 `httpTimeoutMs`，成功条件仍为 HTTP 200–299

## 2. 兼容测试

- [x] 2.1 用 Hutool 规则生成一段已知密文，断言替换实现能解回原文，并且加密后再解密得到原文
- [x] 2.2 断言 HMAC 与 JDK 独立计算的 `sha256=` 头一致

## 3. 去掉依赖与 Native 提示

- [x] 3.1 从 `cat2bug-platform-system/pom.xml` 删除 `hutool-all` 与 `bcprov-jdk18on`，从根 `pom.xml` 的 dependencyManagement 删除 Hutool（若已无引用）
- [x] 3.2 从 `NativeRuntimeInitFeature`、`application-native.properties`、admin `pom.xml` native profile 删除 `SmUtil`、`DefaultSSLInfo`、`HttpGlobalConfig`、`NanoId`
- [x] 3.3 `mvn -pl cat2bug-platform-admin -am dependency:tree` 确认没有 `cn.hutool` 与 `org.bouncycastle`

## 4. 回写长期知识

- [x] 4.1 无新 ADR、无新踩坑。ADR-004 保留指向本变更的一句，并记下 2026-10-17 重打结果（UPX 40.8 MiB）；不改写已接受的约 38.8 MiB
