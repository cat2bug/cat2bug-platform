package com.cat2bug.system.mapper;

import com.cat2bug.system.domain.SysGitPullRequestComment;

import java.util.List;

/**
 * Git Pull Request 讨论评论 Mapper
 */
public interface SysGitPullRequestCommentMapper
{
    int insert(SysGitPullRequestComment comment);

    List<SysGitPullRequestComment> selectListByPullRequestId(Long pullRequestId);
}
