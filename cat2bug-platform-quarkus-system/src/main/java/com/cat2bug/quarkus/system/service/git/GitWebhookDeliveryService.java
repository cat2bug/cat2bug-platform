package com.cat2bug.quarkus.system.service.git;

import jakarta.enterprise.context.ApplicationScoped;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.cat2bug.quarkus.common.util.DateUtils;
import com.cat2bug.quarkus.system.config.GitWebhookAsyncConfig;
import com.cat2bug.quarkus.system.config.GitWebhookProperties;
import com.cat2bug.quarkus.common.domain.SysGitRepositoryWebhook;
import com.cat2bug.quarkus.common.domain.SysGitWebhookDelivery;
import com.cat2bug.quarkus.system.mapper.SysGitRepositoryWebhookMapper;
import com.cat2bug.quarkus.system.mapper.SysGitWebhookDeliveryMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * 出站 HTTP 投递、签名与日志持久化。
 */
@ApplicationScoped
public class GitWebhookDeliveryService
{
    private static final Logger log = LoggerFactory.getLogger(GitWebhookDeliveryService.class);

    private final SysGitRepositoryWebhookMapper webhookMapper;
    private final SysGitWebhookDeliveryMapper deliveryMapper;
    private final GitWebhookSecretCipher secretCipher;
    private final GitWebhookProperties properties;

    public GitWebhookDeliveryService(SysGitRepositoryWebhookMapper webhookMapper,
            SysGitWebhookDeliveryMapper deliveryMapper,
            GitWebhookSecretCipher secretCipher,
            GitWebhookProperties properties)
    {
        this.webhookMapper = webhookMapper;
        this.deliveryMapper = deliveryMapper;
        this.secretCipher = secretCipher;
        this.properties = properties;
    }

    public void deliverAsync(Long webhookId, String githubEvent, String payloadJson)
    {
        try
        {
            deliverSync(webhookId, githubEvent, payloadJson);
        }
        catch (Exception e)
        {
            log.warn("Webhook 异步投递异常：webhookId={} event={} msg={}", webhookId, githubEvent, e.toString());
        }
    }

    void deliverSync(Long webhookId, String githubEvent, String payloadJson)
    {
        SysGitRepositoryWebhook webhook = webhookMapper.selectByWebhookId(webhookId);
        if (webhook == null || !webhook.isActiveFlag())
        {
            return;
        }
        String secret = secretCipher.decrypt(webhook.getSecretCipher());
        String deliveryGuid = UUID.randomUUID().toString().replace("-", "");
        String signature = GitWebhookSigning.signatureHeaderValue(secret, payloadJson);

        long start = System.currentTimeMillis();
        int attempts = properties.getMaxRetries() + 1;
        HttpOutcome lastOutcome = null;
        for (int attempt = 0; attempt < attempts; attempt++)
        {
            if (attempt > 0)
            {
                sleepBackoff(attempt);
            }
            lastOutcome = executePost(webhook.getUrl(), githubEvent, deliveryGuid, signature, payloadJson);
            if (lastOutcome.success)
            {
                break;
            }
        }
        long duration = System.currentTimeMillis() - start;
        persistDelivery(webhook, githubEvent, deliveryGuid, payloadJson, signature, lastOutcome, duration);
        trimDeliveries(webhook.getWebhookId());
    }

    private HttpOutcome executePost(String url, String githubEvent, String deliveryGuid, String signature,
            String payloadJson)
    {
        try
        {
            HttpResponse response = HttpRequest.post(url)
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "Cat2Bug-Webhooks")
                    .header("X-GitHub-Event", githubEvent)
                    .header("X-GitHub-Delivery", deliveryGuid)
                    .header("X-Hub-Signature-256", signature)
                    .body(payloadJson)
                    .timeout(properties.getHttpTimeoutMs())
                    .setFollowRedirects(false)
                    .execute();
            int status = response.getStatus();
            String body = response.body();
            boolean ok = status >= 200 && status < 300;
            return new HttpOutcome(ok, status, body, null);
        }
        catch (Exception e)
        {
            return new HttpOutcome(false, null, null, e.getMessage());
        }
    }

    private void persistDelivery(SysGitRepositoryWebhook webhook, String githubEvent, String deliveryGuid,
            String payloadJson, String signature, HttpOutcome outcome, long durationMs)
    {
        SysGitWebhookDelivery row = new SysGitWebhookDelivery();
        row.setWebhookId(webhook.getWebhookId());
        row.setRepositoryId(webhook.getRepositoryId());
        row.setEvent(githubEvent);
        row.setDeliveryGuid(deliveryGuid);
        row.setRequestHeadersSummary(truncate(
                "X-GitHub-Event=" + githubEvent + "; X-GitHub-Delivery=" + deliveryGuid
                        + "; X-Hub-Signature-256=" + signature,
                2000));
        row.setRequestBody(truncate(payloadJson, properties.getMaxStoredBodyChars()));
        row.setResponseStatus(outcome.status);
        row.setResponseBodyTruncated(truncate(outcome.body, properties.getMaxStoredBodyChars()));
        row.setDurationMs(durationMs);
        row.setSuccess(outcome.success ? 1 : 0);
        row.setErrorMessage(truncate(outcome.errorMessage, 2000));
        row.setCreateTime(DateUtils.getNowDate());
        deliveryMapper.insert(row);
    }

    private void trimDeliveries(Long webhookId)
    {
        try
        {
            deliveryMapper.deleteOlderThanKeepCount(webhookId, properties.getDeliveryKeepCount());
        }
        catch (Exception e)
        {
            log.debug("Webhook 投递日志裁剪跳过：webhookId={} {}", webhookId, e.toString());
        }
    }

    private static void sleepBackoff(int attempt)
    {
        try
        {
            Thread.sleep(500L * (1L << (attempt - 1)));
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
    }

    private static String truncate(String value, int max)
    {
        if (value == null || max <= 0)
        {
            return value;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private static final class HttpOutcome
    {
        private final boolean success;
        private final Integer status;
        private final String body;
        private final String errorMessage;

        private HttpOutcome(boolean success, Integer status, String body, String errorMessage)
        {
            this.success = success;
            this.status = status;
            this.body = body;
            this.errorMessage = errorMessage;
        }
    }
}
