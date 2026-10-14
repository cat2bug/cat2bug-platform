package com.cat2bug.framework.security;

import com.cat2bug.common.constant.SecurityConstants;
import com.cat2bug.common.domain.LoginUser;
import com.cat2bug.common.domain.SysUser;
import com.cat2bug.common.util.StringUtils;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Set;

/**
 * 与 Spring {@code PermissionService}（@ss）语义对齐。
 */
@ApplicationScoped
public class PermissionChecker {

    public boolean hasPermi(LoginUser loginUser, String permission) {
        if (StringUtils.isEmpty(permission) || loginUser == null) {
            return false;
        }
        if (isSuperAdmin(loginUser)) {
            return true;
        }
        Set<String> permissions = loginUser.getPermissions();
        if (permissions == null || permissions.isEmpty()) {
            return false;
        }
        return permissions.contains(SecurityConstants.ALL_PERMISSION)
                || permissions.contains(permission.trim());
    }

    public boolean hasAnyPermi(LoginUser loginUser, String[] permissions) {
        if (permissions == null || permissions.length == 0) {
            return false;
        }
        for (String permission : permissions) {
            if (hasPermi(loginUser, permission)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSuperAdmin(LoginUser loginUser) {
        if (SysUser.isAdmin(loginUser.getUserId())) {
            return true;
        }
        SysUser user = loginUser.getUser();
        if (user != null && SysUser.isAdmin(user.getUserId())) {
            return true;
        }
        if (user != null && user.getRoles() != null) {
            for (com.cat2bug.common.domain.SysRole role : user.getRoles()) {
                if (role.isAdmin() || SecurityConstants.SUPER_ADMIN.equals(role.getRoleKey())) {
                    return true;
                }
            }
        }
        return false;
    }
}
