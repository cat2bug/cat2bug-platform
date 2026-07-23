package com.cat2bug.system.git.browse.service;

import com.cat2bug.system.git.browse.domain.GitBlobContent;
import com.cat2bug.system.git.browse.domain.GitBranchInfo;
import com.cat2bug.system.git.browse.domain.GitCommitDetail;
import com.cat2bug.system.git.browse.domain.GitCommitInfo;
import com.cat2bug.system.git.browse.domain.GitContributorInfo;
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
     * @param nameFilter 按分支名称包含匹配（不区分大小写），为空时返回全部
     */
    List<GitBranchInfo> listBranches(Long repositoryId, String nameFilter);

    GitBranchInfo createBranch(Long repositoryId, String name, String fromRef);

    void deleteBranch(Long repositoryId, String name);

    void setDefaultBranch(Long repositoryId, String name);

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

    /**
     * 提交列表（可按作者与日期过滤；过滤后再应用 limit）。
     *
     * @param author 作者 email 或 name（与贡献者下拉口径一致），空则不过滤
     * @param since  起始日 {@code yyyy-MM-dd}（含当天 00:00:00），空则无下界
     * @param until  结束日 {@code yyyy-MM-dd}（含当天 23:59:59），空则无上界
     */
    List<GitCommitInfo> listCommits(Long repositoryId, String ref, Integer limit, String author, String since,
            String until);

    /**
     * 统计指定 ref 可达的提交总数（不受 listCommits 的 limit 截断影响）。
     *
     * @param ref 分支/tag/commit-ish，为空时使用仓库默认分支
     */
    int countCommits(Long repositoryId, String ref);

    /**
     * 单条提交详情（全文消息 + 相对 first parent 的文件 diff，含 patch 大小限制）。
     *
     * @param commitId 完整或仓库内可解析的缩写 commit id
     */
    GitCommitDetail getCommitDetail(Long repositoryId, String commitId);

    /**
     * 按提交作者聚合贡献者（扫描最近若干提交，返回前若干作者）。
     *
     * @param ref   分支/tag/commit-ish，为空时使用仓库默认分支
     * @param limit 最大返回贡献者条数，未传时使用服务内默认值（见实现类常量）
     */
    List<GitContributorInfo> listContributors(Long repositoryId, String ref, Integer limit);

    /**
     * 将指定 ref（分支/tag/commit）打成源码包写入响应流。
     *
     * @param format {@code zip} 或 {@code tar.gz}
     */
    void writeArchive(Long repositoryId, String ref, String format, jakarta.servlet.http.HttpServletResponse response)
            throws java.io.IOException;

    /**
     * 将指定 ref 下的文件字节流写入响应（供 README 等 Markdown 内相对图片引用）。
     *
     * @param path 仓库内相对路径
     */
    void writeRawBlob(Long repositoryId, String ref, String path, jakarta.servlet.http.HttpServletResponse response)
            throws java.io.IOException;
}
