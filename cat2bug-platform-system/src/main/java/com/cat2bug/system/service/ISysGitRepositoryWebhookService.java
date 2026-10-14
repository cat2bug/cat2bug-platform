package com.cat2bug.system.service;

import com.cat2bug.common.domain.SysGitWebhookDelivery;
import com.cat2bug.common.domain.git.SysGitRepositoryWebhookRequest;
import com.cat2bug.common.domain.git.SysGitRepositoryWebhookSecretReveal;
import com.cat2bug.common.domain.git.SysGitRepositoryWebhookVo;

import java.util.List;

public interface ISysGitRepositoryWebhookService
{
    List<SysGitRepositoryWebhookVo> listWebhooks(Long repositoryId);

    SysGitRepositoryWebhookVo getWebhook(Long repositoryId, Long webhookId);

    SysGitRepositoryWebhookSecretReveal createWebhook(Long repositoryId, SysGitRepositoryWebhookRequest request);

    SysGitRepositoryWebhookVo updateWebhook(Long repositoryId, Long webhookId, SysGitRepositoryWebhookRequest request);

    int deleteWebhook(Long repositoryId, Long webhookId);

    SysGitRepositoryWebhookSecretReveal regenerateSecret(Long repositoryId, Long webhookId);

    List<SysGitWebhookDelivery> listDeliveries(Long repositoryId, Long webhookId);

    void sendTestPing(Long repositoryId, Long webhookId);
}
