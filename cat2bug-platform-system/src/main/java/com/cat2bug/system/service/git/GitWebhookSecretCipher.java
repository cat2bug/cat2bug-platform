package com.cat2bug.system.service.git;

import jakarta.enterprise.context.ApplicationScoped;

import com.cat2bug.system.config.GitWebhookProperties;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * Webhook secret 可逆加密（AES/ECB/PKCS5Padding）。
 * 密钥为配置项 SHA-256 的前 16 字节，密文 Base64，明文 UTF-8。与原先 Hutool {@code SecureUtil.aes} 一致。
 */
@ApplicationScoped
public class GitWebhookSecretCipher
{
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";
    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789";

    private final byte[] key;

    public GitWebhookSecretCipher(GitWebhookProperties properties)
    {
        this.key = Arrays.copyOf(sha256(properties.getSecretKey()), 16);
    }

    public String encrypt(String plaintext)
    {
        try
        {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        }
        catch (GeneralSecurityException e)
        {
            throw new IllegalStateException("Webhook secret 加密失败", e);
        }
    }

    public String decrypt(String cipherText)
    {
        try
        {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"));
            byte[] plain = cipher.doFinal(Base64.getDecoder().decode(cipherText));
            return new String(plain, StandardCharsets.UTF_8);
        }
        catch (GeneralSecurityException e)
        {
            throw new IllegalStateException("Webhook secret 解密失败", e);
        }
    }

    public static String generatePlainSecret()
    {
        SecureRandom random = new SecureRandom();
        char[] chars = new char[32];
        for (int i = 0; i < chars.length; i++)
        {
            chars[i] = ALPHABET.charAt(random.nextInt(ALPHABET.length()));
        }
        return new String(chars);
    }

    private static byte[] sha256(String value)
    {
        try
        {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        }
        catch (GeneralSecurityException e)
        {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}
