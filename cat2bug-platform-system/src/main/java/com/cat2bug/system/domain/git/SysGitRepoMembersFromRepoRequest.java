package com.cat2bug.system.domain.git;

import java.util.List;

/**
 * 从同项目其他仓库批量导入成员请求体。
 */
public class SysGitRepoMembersFromRepoRequest
{
    private Long sourceRepositoryId;

    private List<Long> userIds;

    /** read | write | admin */
    private String role;

    public Long getSourceRepositoryId()
    {
        return sourceRepositoryId;
    }

    public void setSourceRepositoryId(Long sourceRepositoryId)
    {
        this.sourceRepositoryId = sourceRepositoryId;
    }

    public List<Long> getUserIds()
    {
        return userIds;
    }

    public void setUserIds(List<Long> userIds)
    {
        this.userIds = userIds;
    }

    public String getRole()
    {
        return role;
    }

    public void setRole(String role)
    {
        this.role = role;
    }
}
