package com.cat2bug.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.cat2bug.common.annotation.Excel;
import com.cat2bug.common.core.domain.BaseEntity;

/**
 * 项目代码仓库对象 sys_git_repository
 *
 * 每条记录对应项目下的一个「工程」：元数据落库 + 磁盘上的一个 JGit bare 仓库。
 *
 * @author cat2bug
 */
public class SysGitRepository extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 仓库ID */
    private Long repositoryId;

    /** 项目ID */
    @Excel(name = "项目ID")
    private Long projectId;

    /** 显示名称 */
    @Excel(name = "仓库名称")
    private String name;

    /** URL slug（项目内唯一，用于拼装 clone 地址） */
    @Excel(name = "slug")
    private String slug;

    /**
     * 磁盘绝对路径（bare 仓）。
     * 创建时可留空由服务端按默认规则计算；manage 用户也可显式指定绝对路径（服务端会规范化并校验可写性）。
     */
    private String diskPath;

    /** 默认分支名（空仓可由首次 push 决定） */
    @Excel(name = "默认分支")
    private String defaultBranch;

    /**
     * 可见性：{@code public}=开源（所有人可见），{@code project}=项目内部开源。
     */
    private String visibility;

    /** 所属团队 path key（查询时 join，非持久化） */
    private String teamKey;

    /** 所属项目 path key（查询时 join，非持久化） */
    private String projectKey;

    /** 当前用户是否关注该仓库（查询时 join，非持久化） */
    private Boolean collect;

    /** 关注该仓库的用户数（查询时子查询聚合，非持久化） */
    private Integer collectCount;

    /** 本地 heads 分支数（列表查询时由 JGit 统计，非持久化） */
    private Integer branchCount;

    /**
     * 本次更新是否发生了 slug 重命名（服务端计算后回填，非持久化字段）。
     * 供 Controller 组装「旧 clone 地址已失效」提示，不参与数据库读写。
     */
    private boolean slugChanged;

    public Long getRepositoryId()
    {
        return repositoryId;
    }

    public void setRepositoryId(Long repositoryId)
    {
        this.repositoryId = repositoryId;
    }

    public Long getProjectId()
    {
        return projectId;
    }

    public void setProjectId(Long projectId)
    {
        this.projectId = projectId;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getSlug()
    {
        return slug;
    }

    public void setSlug(String slug)
    {
        this.slug = slug;
    }

    public String getDiskPath()
    {
        return diskPath;
    }

    public void setDiskPath(String diskPath)
    {
        this.diskPath = diskPath;
    }

    public String getDefaultBranch()
    {
        return defaultBranch;
    }

    public void setDefaultBranch(String defaultBranch)
    {
        this.defaultBranch = defaultBranch;
    }

    public String getVisibility()
    {
        return visibility;
    }

    public void setVisibility(String visibility)
    {
        this.visibility = visibility;
    }

    public String getTeamKey()
    {
        return teamKey;
    }

    public void setTeamKey(String teamKey)
    {
        this.teamKey = teamKey;
    }

    public String getProjectKey()
    {
        return projectKey;
    }

    public void setProjectKey(String projectKey)
    {
        this.projectKey = projectKey;
    }

    public Boolean getCollect()
    {
        return collect;
    }

    public void setCollect(Boolean collect)
    {
        this.collect = collect;
    }

    public Integer getCollectCount()
    {
        return collectCount;
    }

    public void setCollectCount(Integer collectCount)
    {
        this.collectCount = collectCount;
    }

    public Integer getBranchCount()
    {
        return branchCount;
    }

    public void setBranchCount(Integer branchCount)
    {
        this.branchCount = branchCount;
    }

    public boolean isSlugChanged()
    {
        return slugChanged;
    }

    public void setSlugChanged(boolean slugChanged)
    {
        this.slugChanged = slugChanged;
    }

    /**
     * clone 地址（Smart HTTP）：{@code /{teamKey}/{projectKey}/code/{slug}.git}。
     * 由 teamKey、projectKey、slug 计算得出，不落库；任一键或 slug 缺失时返回 null。
     */
    public String getCloneUrl()
    {
        if (teamKey == null || teamKey.isEmpty()
                || projectKey == null || projectKey.isEmpty()
                || slug == null || slug.isEmpty())
        {
            return null;
        }
        return "/" + teamKey + "/" + projectKey + "/code/" + slug + ".git";
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("repositoryId", getRepositoryId())
            .append("projectId", getProjectId())
            .append("name", getName())
            .append("slug", getSlug())
            .append("diskPath", getDiskPath())
            .append("defaultBranch", getDefaultBranch())
            .append("visibility", getVisibility())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
