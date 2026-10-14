package com.cat2bug.system.service.mcp;

import com.cat2bug.common.constant.SecurityConstants;
import com.cat2bug.common.domain.SysProject;
import com.cat2bug.common.domain.SysRole;
import com.cat2bug.common.security.McpModules;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 某用户在某项目内经 MCP 的有效权限：角色权限 ∩ 项目已勾选模块。
 *
 * @param rolePermissions 该用户在项目（及所属团队、系统角色）下的角色权限并集，可能含 {@code *:*:*}
 */
public record McpProjectAccess(Long userId, SysProject project, boolean admin, boolean member, boolean enabled,
                               List<String> modules, List<SysRole> roles, Set<String> rolePermissions) {

    public Long projectId() {
        return project != null ? project.getProjectId() : null;
    }

    public boolean moduleSelected(String moduleKey) {
        return enabled && moduleKey != null && modules.contains(moduleKey);
    }

    /** 单个权限串：所属模块已勾选，且角色拥有该权限（或全部权限）。 */
    public boolean allows(String permission) {
        String module = McpModules.moduleOf(permission);
        if (!moduleSelected(module) || !(admin || member)) {
            return false;
        }
        return admin || rolePermissions.contains(SecurityConstants.ALL_PERMISSION) || rolePermissions.contains(permission);
    }

    /** 只看角色、不看 MCP 模块勾选（用于网页上管理该项目的 MCP 设置）。 */
    public boolean hasRolePermission(String permission) {
        if (!(admin || member)) {
            return false;
        }
        return admin || rolePermissions.contains(SecurityConstants.ALL_PERMISSION) || rolePermissions.contains(permission);
    }

    public boolean allowsAny(Collection<String> permissions) {
        for (String p : permissions) {
            if (allows(p)) {
                return true;
            }
        }
        return false;
    }

    public boolean allowsAll(Collection<String> permissions) {
        if (permissions.isEmpty()) {
            return false;
        }
        for (String p : permissions) {
            if (!allows(p)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 写入内部会话的权限集合：只保留已勾选模块内的权限。持有 {@code *:*:*} 时原样保留，
     * 由 MCP 层在转发前按 {@link #allows} 预先校验。
     */
    public Set<String> sessionPermissions() {
        Set<String> out = new HashSet<>();
        if (admin || rolePermissions.contains(SecurityConstants.ALL_PERMISSION)) {
            out.add(SecurityConstants.ALL_PERMISSION);
            return out;
        }
        for (String p : rolePermissions) {
            if (moduleSelected(McpModules.moduleOf(p))) {
                out.add(p);
            }
        }
        return out;
    }
}
