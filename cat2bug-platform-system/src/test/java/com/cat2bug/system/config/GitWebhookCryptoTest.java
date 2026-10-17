package com.cat2bug.system.config;

import com.cat2bug.system.service.git.GitWebhookSecretCipher;
import com.cat2bug.system.service.git.GitWebhookSigning;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 与 Hutool {@code SecureUtil.aes} / HMAC-SHA256 的已知向量对照。
 * 密文由 OpenSSL AES-128-ECB + PKCS#7 生成，密钥为 SHA-256("cat2bug-test-webhook-key") 的前 16 字节。
 */
class GitWebhookCryptoTest {

    private static final String SECRET_KEY = "cat2bug-test-webhook-key";
    private static final String PLAIN_SECRET = "webhook-plain-secret";
    /** Hutool 规则下的已知密文（AES/ECB/PKCS5Padding，Base64）。 */
    private static final String HUTOOL_CIPHERTEXT = "PFW7+lhyhKwW+ZjWcmogkgZJeL2S9n9CkLXUMDdInmQ=";

    @Test
    void decryptsHutoolCiphertextAndRoundTrips() {
        GitWebhookSecretCipher cipher = cipher();
        assertEquals(PLAIN_SECRET, cipher.decrypt(HUTOOL_CIPHERTEXT));
        assertEquals(PLAIN_SECRET, cipher.decrypt(cipher.encrypt(PLAIN_SECRET)));
    }

    @Test
    void generatedSecretIs32LowercaseAlphanumerics() {
        String secret = GitWebhookSecretCipher.generatePlainSecret();
        assertEquals(32, secret.length());
        assertTrue(secret.matches("[a-z0-9]{32}"));
    }

    @Test
    void signatureMatchesIndependentHmac() throws Exception {
        String secret = "plain-secret";
        String body = "payload";
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] digest = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            hex.append(String.format("%02x", b));
        }
        String header = "sha256=" + hex;
        assertEquals("sha256=c4c667bdbb2c79fe82e6067dc78f6883fda7ced38b344792a4ef67125fc74cb6", header);
        assertEquals(header, GitWebhookSigning.signatureHeaderValue(secret, body));
        assertTrue(GitWebhookSigning.verify(secret, body, header));
    }

    private static GitWebhookSecretCipher cipher() {
        GitWebhookProperties properties = new GitWebhookProperties();
        properties.secretKey = SECRET_KEY;
        return new GitWebhookSecretCipher(properties);
    }
}
