package com.cat2bug.system.domain.git;

/**
 * 添加 / 更新仓库成员请求体。
 */
public class SysGitRepositoryMemberRequest
{
    private Long userId;

    /** read | write | admin */
    private String role;

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
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
