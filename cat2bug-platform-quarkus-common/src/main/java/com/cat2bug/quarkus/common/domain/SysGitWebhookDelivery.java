package com.cat2bug.quarkus.common.domain;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;

/**
 * Webhook 投递日志 {@code sys_git_webhook_delivery}
 */
public class SysGitWebhookDelivery
{
    private Long deliveryId;

    private Long webhookId;

    private Long repositoryId;

    private String event;

    private String deliveryGuid;

    private String requestHeadersSummary;

    private String requestBody;

    private Integer responseStatus;

    private String responseBodyTruncated;

    private Long durationMs;

    private Integer success;

    private String errorMessage;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    public Long getDeliveryId()
    {
        return deliveryId;
    }

    public void setDeliveryId(Long deliveryId)
    {
        this.deliveryId = deliveryId;
    }

    public Long getWebhookId()
    {
        return webhookId;
    }

    public void setWebhookId(Long webhookId)
    {
        this.webhookId = webhookId;
    }

    public Long getRepositoryId()
    {
        return repositoryId;
    }

    public void setRepositoryId(Long repositoryId)
    {
        this.repositoryId = repositoryId;
    }

    public String getEvent()
    {
        return event;
    }

    public void setEvent(String event)
    {
        this.event = event;
    }

    public String getDeliveryGuid()
    {
        return deliveryGuid;
    }

    public void setDeliveryGuid(String deliveryGuid)
    {
        this.deliveryGuid = deliveryGuid;
    }

    public String getRequestHeadersSummary()
    {
        return requestHeadersSummary;
    }

    public void setRequestHeadersSummary(String requestHeadersSummary)
    {
        this.requestHeadersSummary = requestHeadersSummary;
    }

    public String getRequestBody()
    {
        return requestBody;
    }

    public void setRequestBody(String requestBody)
    {
        this.requestBody = requestBody;
    }

    public Integer getResponseStatus()
    {
        return responseStatus;
    }

    public void setResponseStatus(Integer responseStatus)
    {
        this.responseStatus = responseStatus;
    }

    public String getResponseBodyTruncated()
    {
        return responseBodyTruncated;
    }

    public void setResponseBodyTruncated(String responseBodyTruncated)
    {
        this.responseBodyTruncated = responseBodyTruncated;
    }

    public Long getDurationMs()
    {
        return durationMs;
    }

    public void setDurationMs(Long durationMs)
    {
        this.durationMs = durationMs;
    }

    public Integer getSuccess()
    {
        return success;
    }

    public void setSuccess(Integer success)
    {
        this.success = success;
    }

    public String getErrorMessage()
    {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage)
    {
        this.errorMessage = errorMessage;
    }

    public Date getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }
}
