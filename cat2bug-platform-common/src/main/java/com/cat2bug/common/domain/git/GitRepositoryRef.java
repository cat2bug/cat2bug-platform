package com.cat2bug.common.domain.git;

/**
 * Git 仓库的最小只读引用，供 Smart HTTP / 权限判定使用。
 *
 * <p>刻意不是完整的 {@code SysGitRepository} 领域对象（tasks.md 第2节的正式 CRUD 模型）：
 * framework/admin 模块的 Smart HTTP 鉴权与取盘路径只需要这几个字段，用窄 DTO 而不是直接依赖
 * 会随 CRUD 演进的完整领域对象，避免跨模块耦合面过大。</p>
 */
public class GitRepositoryRef
{
    private final Long repositoryId;

    private final Long projectId;

    private final String slug;

    private final String diskPath;

    private final String defaultBranch;

    public GitRepositoryRef(Long repositoryId, Long projectId, String slug, String diskPath, String defaultBranch)
    {
        this.repositoryId = repositoryId;
        this.projectId = projectId;
        this.slug = slug;
        this.diskPath = diskPath;
        this.defaultBranch = defaultBranch;
    }

    public Long getRepositoryId()
    {
        return repositoryId;
    }

    public Long getProjectId()
    {
        return projectId;
    }

    public String getSlug()
    {
        return slug;
    }

    public String getDiskPath()
    {
        return diskPath;
    }

    public String getDefaultBranch()
    {
        return defaultBranch;
    }
}
