package com.cat2bug.system.config;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * 仓级 Webhook 配置（{@code cat2bug.git.webhook.*}）。
 */
@ApplicationScoped
public class GitWebhookProperties {

    @ConfigProperty(name = "cat2bug.git.webhook.secret-key",
            defaultValue = "cat2bug-local-git-webhook-secret-key")
    String secretKey;

    @ConfigProperty(name = "cat2bug.git.webhook.http-timeout-ms", defaultValue = "10000")
    int httpTimeoutMs;

    @ConfigProperty(name = "cat2bug.git.webhook.max-retries", defaultValue = "3")
    int maxRetries;

    @ConfigProperty(name = "cat2bug.git.webhook.delivery-keep-count", defaultValue = "50")
    int deliveryKeepCount;

    @ConfigProperty(name = "cat2bug.git.webhook.max-stored-body-chars", defaultValue = "32768")
    int maxStoredBodyChars;

    public String getSecretKey() {
        return secretKey;
    }

    public int getHttpTimeoutMs() {
        return httpTimeoutMs;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public int getDeliveryKeepCount() {
        return deliveryKeepCount;
    }

    public int getMaxStoredBodyChars() {
        return maxStoredBodyChars;
    }
}
