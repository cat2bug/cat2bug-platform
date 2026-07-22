package com.cat2bug.system.mapper;

import com.cat2bug.system.domain.SysGitRepositoryWebhook;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SysGitRepositoryWebhookMapper
{
    List<SysGitRepositoryWebhook> selectByRepositoryId(Long repositoryId);

    SysGitRepositoryWebhook selectByWebhookId(Long webhookId);

    SysGitRepositoryWebhook selectByRepositoryIdAndWebhookId(
            @Param("repositoryId") Long repositoryId,
            @Param("webhookId") Long webhookId);

    int insert(SysGitRepositoryWebhook row);

    int update(SysGitRepositoryWebhook row);

    int deleteByRepositoryIdAndWebhookId(
            @Param("repositoryId") Long repositoryId,
            @Param("webhookId") Long webhookId);
}
