package com.cat2bug.framework.security;

import com.cat2bug.common.domain.LoginUser;
import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class AuthContext {

    private LoginUser loginUser;

    /** 本次请求通过校验的 {@link RequiresPermissions} 权限串（逗号分隔），数据范围据此筛选角色 */
    private String permissionContext;

    public LoginUser getLoginUser() {
        return loginUser;
    }

    public void setLoginUser(LoginUser loginUser) {
        this.loginUser = loginUser;
    }

    public boolean isAuthenticated() {
        return loginUser != null;
    }

    public String getPermissionContext() {
        return permissionContext;
    }

    public void setPermissionContext(String permissionContext) {
        this.permissionContext = permissionContext;
    }
}
