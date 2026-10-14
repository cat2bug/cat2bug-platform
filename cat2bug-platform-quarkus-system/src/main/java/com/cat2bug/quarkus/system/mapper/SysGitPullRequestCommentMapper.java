package com.cat2bug.quarkus.system.mapper;

import org.apache.ibatis.annotations.Mapper;
import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import com.cat2bug.quarkus.system.mybatis.QuarkusMapperDataSources;

import com.cat2bug.quarkus.common.domain.SysGitPullRequestComment;

import java.util.List;

/**
 * Git Pull Request 讨论评论 Mapper
 */
@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface SysGitPullRequestCommentMapper
{
    int insert(SysGitPullRequestComment comment);

    List<SysGitPullRequestComment> selectListByPullRequestId(Long pullRequestId);
}
