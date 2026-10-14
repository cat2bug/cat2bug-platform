package com.cat2bug.quarkus.system.mapper;

import org.apache.ibatis.annotations.Mapper;
import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import com.cat2bug.quarkus.system.mybatis.QuarkusMapperDataSources;

import com.cat2bug.quarkus.common.domain.SysGitRepositoryWebhook;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
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
