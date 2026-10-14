package com.cat2bug.quarkus.system.mapper;

import org.apache.ibatis.annotations.Mapper;
import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import com.cat2bug.quarkus.system.mybatis.QuarkusMapperDataSources;

import com.cat2bug.quarkus.common.domain.SysGitPullRequest;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Git Pull Request Mapper
 */
@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface SysGitPullRequestMapper
{
    int insert(SysGitPullRequest pullRequest);

    SysGitPullRequest selectById(Long pullRequestId);

    SysGitPullRequest selectByRepoAndNumber(
            @Param("repositoryId") Long repositoryId,
            @Param("number") Integer number);

    List<SysGitPullRequest> selectListByRepo(
            @Param("repositoryId") Long repositoryId,
            @Param("state") String state);

    List<SysGitPullRequest> selectOpenBySourceBranch(
            @Param("repositoryId") Long repositoryId,
            @Param("sourceBranch") String sourceBranch);

    int updateStateFields(SysGitPullRequest pullRequest);

    /**
     * 返回仓库当前最大 PR 编号；调用方须在事务内串行分配下一个编号。
     */
    Integer selectMaxNumberByRepo(Long repositoryId);
}
