package com.cat2bug.system.git.browse.service;

import com.cat2bug.system.git.browse.domain.GitBlobContent;
import com.cat2bug.system.git.browse.domain.GitBranchInfo;
import com.cat2bug.system.git.browse.domain.GitCommitInfo;
import com.cat2bug.system.git.browse.domain.GitTreeEntryInfo;

import java.util.List;

/**
 * 项目 Git 代码仓库浏览服务：分支 / 目录 / 文件内容 / 提交记录（只读，pull 权限）。
 *
 * <p>对应 OpenSpec {@code git-repo-browse} 规格；仓库元数据的增删改查由
 * {@code ISysGitRepositoryService}（任务 2.x）负责，本服务仅基于其落库的 {@code disk_path}
 * 用 JGit 只读打开 bare 仓，不重复维护仓库领域模型。权限矩阵与 CRUD/Smart HTTP 校验逻辑一致：
 * 系统管理员若无目标项目角色同样被拒绝（无绕过）。</p>
 */
public interface ISysGitBrowseService
{
    List<GitBranchInfo> listBranches(Long repositoryId);

    /**
     * @param ref  分支/tag/commit-ish，为空时使用仓库默认分支
     * @param path 目录路径，为空或 "/" 表示根目录
     */
    List<GitTreeEntryInfo> listTree(Long repositoryId, String ref, String path);

    GitBlobContent readBlob(Long repositoryId, String ref, String path);

    /**
     * @param limit 最大返回条数，未传时使用默认值
     */
    List<GitCommitInfo> listCommits(Long repositoryId, String ref, Integer limit);
}
