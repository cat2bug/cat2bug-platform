package com.cat2bug.system.mapper;

import com.cat2bug.system.domain.SysGitPullRequest;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Git Pull Request Mapper
 */
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

    int updateStateFields(SysGitPullRequest pullRequest);

    /**
     * 返回仓库当前最大 PR 编号；调用方须在事务内串行分配下一个编号。
     */
    Integer selectMaxNumberByRepo(Long repositoryId);
}
