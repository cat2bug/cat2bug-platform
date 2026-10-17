## ADDED Requirements

### Requirement: Native runtime does not ship Hutool or BouncyCastle

The Quarkus admin runtime classpath MUST NOT include `cn.hutool:hutool-all` or `org.bouncycastle:bcprov-jdk18on`. Git webhook secret storage, HMAC-SHA256 signing, and outbound delivery MUST keep the behavior required by `git-repo-webhooks`. Secrets already stored with Hutool AES MUST decrypt with the JDK replacement: algorithm `AES/ECB/PKCS5Padding`, key equal to the first 16 bytes of SHA-256 of the configured secret key, ciphertext Base64, plaintext UTF-8.

#### Scenario: Dependency tree has neither library

- **WHEN** `mvn -pl cat2bug-platform-admin -am dependency:tree` is run for the admin module
- **THEN** the tree contains no `cn.hutool:hutool-all` and no `org.bouncycastle:bcprov-jdk18on`

#### Scenario: Existing webhook ciphertext still decrypts

- **WHEN** a webhook secret ciphertext produced by Hutool `SecureUtil.aes` under the rules above is decrypted by the replacement
- **THEN** the plaintext secret matches the original value

#### Scenario: Delivery signature stays GitHub-compatible

- **WHEN** a webhook delivery is sent
- **THEN** `X-Hub-Signature-256` is `sha256=` plus the lowercase hex HMAC-SHA256 of the raw body using the webhook secret
