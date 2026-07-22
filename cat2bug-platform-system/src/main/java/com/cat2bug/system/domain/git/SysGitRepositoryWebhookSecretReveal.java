package com.cat2bug.system.domain.git;

/**
 * 创建或重置 secret 时的一次性明文回传
 */
public class SysGitRepositoryWebhookSecretReveal
{
    private SysGitRepositoryWebhookVo webhook;

    private String secret;

    public SysGitRepositoryWebhookVo getWebhook()
    {
        return webhook;
    }

    public void setWebhook(SysGitRepositoryWebhookVo webhook)
    {
        this.webhook = webhook;
    }

    public String getSecret()
    {
        return secret;
    }

    public void setSecret(String secret)
    {
        this.secret = secret;
    }
}
