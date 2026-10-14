package com.cat2bug.quarkus.common.domain.git;

/**
 * 当前用户对某仓库的有效权限（供前端 Tab / 守卫）。
 */
public class GitRepoMyAccess
{
    /** 有效角色：read | write | admin；无权限时为 null */
    private String role;

    private boolean canPull;

    private boolean canPush;

    private boolean canManage;

    public GitRepoMyAccess()
    {
    }

    public GitRepoMyAccess(String role, boolean canPull, boolean canPush, boolean canManage)
    {
        this.role = role;
        this.canPull = canPull;
        this.canPush = canPush;
        this.canManage = canManage;
    }

    public String getRole()
    {
        return role;
    }

    public void setRole(String role)
    {
        this.role = role;
    }

    public boolean isCanPull()
    {
        return canPull;
    }

    public void setCanPull(boolean canPull)
    {
        this.canPull = canPull;
    }

    public boolean isCanPush()
    {
        return canPush;
    }

    public void setCanPush(boolean canPush)
    {
        this.canPush = canPush;
    }

    public boolean isCanManage()
    {
        return canManage;
    }

    public void setCanManage(boolean canManage)
    {
        this.canManage = canManage;
    }
}
