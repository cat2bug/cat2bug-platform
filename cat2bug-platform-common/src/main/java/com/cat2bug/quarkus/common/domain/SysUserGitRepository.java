package com.cat2bug.quarkus.common.domain;

/**
 * 用户关注的代码仓库 sys_user_git_repository
 */
public class SysUserGitRepository
{
    private Long userGitRepositoryId;
    private Long userId;
    private Long repositoryId;
    /** 是否关注 */
    private Boolean collect;
    private String createBy;
    private String updateBy;

    public Long getUserGitRepositoryId()
    {
        return userGitRepositoryId;
    }

    public void setUserGitRepositoryId(Long userGitRepositoryId)
    {
        this.userGitRepositoryId = userGitRepositoryId;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public Long getRepositoryId()
    {
        return repositoryId;
    }

    public void setRepositoryId(Long repositoryId)
    {
        this.repositoryId = repositoryId;
    }

    public Boolean getCollect()
    {
        return collect;
    }

    public void setCollect(Boolean collect)
    {
        this.collect = collect;
    }

    public String getCreateBy()
    {
        return createBy;
    }

    public void setCreateBy(String createBy)
    {
        this.createBy = createBy;
    }

    public String getUpdateBy()
    {
        return updateBy;
    }

    public void setUpdateBy(String updateBy)
    {
        this.updateBy = updateBy;
    }
}
