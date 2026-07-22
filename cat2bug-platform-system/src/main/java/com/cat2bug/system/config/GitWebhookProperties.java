package com.cat2bug.system.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 仓级 Webhook 配置（{@code cat2bug.git.webhook.*}）。
 */
@Component
@ConfigurationProperties(prefix = "cat2bug.git.webhook")
public class GitWebhookProperties
{
    /**
     * 用于 AES 加密 webhook secret 的主密钥；生产环境务必覆盖默认值。
     */
    private String secretKey = "cat2bug-local-git-webhook-secret-key";

    /** HTTP 投递超时（毫秒） */
    private int httpTimeoutMs = 10_000;

    /** 失败重试次数（不含首次） */
    private int maxRetries = 3;

    /** 每个 webhook 保留的投递日志条数 */
    private int deliveryKeepCount = 50;

    /** 存储/日志中请求或响应 body 最大字符数 */
    private int maxStoredBodyChars = 32 * 1024;

    public String getSecretKey()
    {
        return secretKey;
    }

    public void setSecretKey(String secretKey)
    {
        this.secretKey = secretKey;
    }

    public int getHttpTimeoutMs()
    {
        return httpTimeoutMs;
    }

    public void setHttpTimeoutMs(int httpTimeoutMs)
    {
        this.httpTimeoutMs = httpTimeoutMs;
    }

    public int getMaxRetries()
    {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries)
    {
        this.maxRetries = maxRetries;
    }

    public int getDeliveryKeepCount()
    {
        return deliveryKeepCount;
    }

    public void setDeliveryKeepCount(int deliveryKeepCount)
    {
        this.deliveryKeepCount = deliveryKeepCount;
    }

    public int getMaxStoredBodyChars()
    {
        return maxStoredBodyChars;
    }

    public void setMaxStoredBodyChars(int maxStoredBodyChars)
    {
        this.maxStoredBodyChars = maxStoredBodyChars;
    }
}
