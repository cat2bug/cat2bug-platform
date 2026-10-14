package com.cat2bug.common.domain.git;

/**
 * 创建仓库 ACL 邀请链接请求体。
 */
public class SysGitRepoInviteCreateRequest
{
    /** read | write | admin */
    private String role;

    public String getRole()
    {
        return role;
    }

    public void setRole(String role)
    {
        this.role = role;
    }
}
