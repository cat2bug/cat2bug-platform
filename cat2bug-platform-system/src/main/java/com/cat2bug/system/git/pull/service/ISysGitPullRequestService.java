package com.cat2bug.system.git.pull.service;

import com.cat2bug.common.domain.SysGitPullRequest;
import com.cat2bug.common.domain.SysGitPullRequestComment;
import com.cat2bug.system.git.browse.domain.GitCommitFileChange;
import com.cat2bug.system.git.browse.domain.GitCommitInfo;

import java.util.List;

/**
 * 仓库 Pull Request 生命周期服务。
 */
public interface ISysGitPullRequestService
{
    SysGitPullRequest createPullRequest(Long repositoryId, SysGitPullRequest pullRequest);

    List<SysGitPullRequest> listPullRequests(Long repositoryId, String state);

    SysGitPullRequest getPullRequest(Long repositoryId, Integer number);

    void closePullRequest(Long repositoryId, Integer number);

    List<GitCommitInfo> listCommits(Long repositoryId, Integer number);

    List<GitCommitFileChange> listFiles(Long repositoryId, Integer number);

    List<SysGitPullRequestComment> listComments(Long repositoryId, Integer number);

    SysGitPullRequestComment createComment(Long repositoryId, Integer number, SysGitPullRequestComment comment);

    SysGitPullRequest mergePullRequest(Long repositoryId, Integer number);
}
