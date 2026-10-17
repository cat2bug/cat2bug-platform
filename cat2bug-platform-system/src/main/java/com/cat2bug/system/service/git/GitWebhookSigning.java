package com.cat2bug.system.service.git;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

/**
 * GitHub {@code X-Hub-Signature-256} 签名。
 */
public final class GitWebhookSigning
{
    private GitWebhookSigning()
    {
    }

    public static String signatureHeaderValue(String secret, String rawBody)
    {
        try
        {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
            return "sha256=" + toHex(digest);
        }
        catch (GeneralSecurityException e)
        {
            throw new IllegalStateException("Webhook HMAC 计算失败", e);
        }
    }

    public static boolean verify(String secret, String rawBody, String headerValue)
    {
        if (headerValue == null || !headerValue.startsWith("sha256="))
        {
            return false;
        }
        String expected = signatureHeaderValue(secret, rawBody);
        return expected.equals(headerValue);
    }

    private static String toHex(byte[] digest)
    {
        char[] hex = new char[digest.length * 2];
        char[] alphabet = "0123456789abcdef".toCharArray();
        for (int i = 0; i < digest.length; i++)
        {
            int value = digest[i] & 0xff;
            hex[i * 2] = alphabet[value >>> 4];
            hex[i * 2 + 1] = alphabet[value & 0x0f];
        }
        return new String(hex);
    }
}
