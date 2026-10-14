package com.cat2bug.quarkus.system.service.git;

import jakarta.enterprise.context.ApplicationScoped;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.crypto.symmetric.AES;
import com.cat2bug.quarkus.system.config.GitWebhookProperties;

import java.util.Arrays;

/**
 * Webhook secret 可逆加密（AES），与 BCrypt 等单向哈希不同。
 */
@ApplicationScoped
public class GitWebhookSecretCipher
{
    private final AES aes;

    public GitWebhookSecretCipher(GitWebhookProperties properties)
    {
        byte[] key = Arrays.copyOf(DigestUtil.sha256(properties.getSecretKey()), 16);
        this.aes = SecureUtil.aes(key);
    }

    public String encrypt(String plaintext)
    {
        return aes.encryptBase64(plaintext);
    }

    public String decrypt(String cipher)
    {
        return aes.decryptStr(cipher);
    }

    public static String generatePlainSecret()
    {
        return RandomUtil.randomString(32);
    }
}
