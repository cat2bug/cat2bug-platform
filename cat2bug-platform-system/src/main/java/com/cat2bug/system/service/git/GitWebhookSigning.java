package com.cat2bug.system.service.git;

import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;

import java.nio.charset.StandardCharsets;

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
        HMac hmac = new HMac(HmacAlgorithm.HmacSHA256, secret.getBytes(StandardCharsets.UTF_8));
        return "sha256=" + hmac.digestHex(rawBody);
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
}
