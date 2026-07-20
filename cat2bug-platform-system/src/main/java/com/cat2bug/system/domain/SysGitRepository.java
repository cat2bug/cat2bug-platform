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

    public boolean isSlugChanged()
    {
        return slugChanged;
    }

    public void setSlugChanged(boolean slugChanged)
    {
        this.slugChanged = slugChanged;
    }

    /**
     * clone 地址（Smart HTTP，任务 3 落地）：{@code /git/p/{projectId}/{slug}.git}。
     * 由 projectId + slug 计算得出，不落库；slug 变更后该值随之变化。
     */
    public String getCloneUrl()
    {
        if (projectId == null || slug == null)
        {
            return null;
        }
        return "/git/p/" + projectId + "/" + slug + ".git";
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
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
