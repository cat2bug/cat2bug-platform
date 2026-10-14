package com.cat2bug.system.security;

import com.cat2bug.common.domain.LoginUser;
import com.cat2bug.common.domain.SysRole;
import com.cat2bug.common.domain.SysUser;
import com.cat2bug.framework.security.AuthContext;
import io.quarkus.arc.Arc;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 按角色数据范围生成 SQL 过滤片段，逻辑对齐原 Spring 线 {@code DataScopeAspect#dataScopeFilter}。
 * <p>
 * 片段以 {@code ${dataScope}} 拼进 Mapper XML，因此只允许拼接常量别名与数字 ID，不得拼接任何请求参数。
 */
@ApplicationScoped
public class DataScopeSqlBuilder {

    public static final String DATA_SCOPE_ALL = "1";
    public static final String DATA_SCOPE_CUSTOM = "2";
    public static final String DATA_SCOPE_DEPT = "3";
    public static final String DATA_SCOPE_DEPT_AND_CHILD = "4";
    public static final String DATA_SCOPE_SELF = "5";

    private static final String ALL_PERMISSION = "*:*:*";
    private static final Pattern SQL_ALIAS = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    @Inject
    AuthContext authContext;

    /**
     * 当前登录用户的数据范围片段。超级管理员、无请求上下文（启动任务等）或未登录时不过滤，返回空串。
     */
    public String forCurrentUser(String deptAlias, String userAlias) {
        if (!Arc.container().requestContext().isActive()) {
            return "";
        }
        LoginUser loginUser = authContext.getLoginUser();
        if (loginUser == null || loginUser.getUser() == null || loginUser.getUser().isAdmin()) {
            return "";
        }
        return build(loginUser.getUser(), deptAlias, userAlias, authContext.getPermissionContext());
    }

    /**
     * @param user       当前用户（需带角色及其 dataScope）
     * @param deptAlias  部门表别名
     * @param userAlias  用户表别名，可为空（此时「仅本人」不返回任何数据）
     * @param permission 本次请求校验的权限串（逗号分隔），角色的权限字符不含其中任何一个时该角色不参与
     * @return 空串（不过滤）或 {@code " AND (...)"}
     */
    public static String build(SysUser user, String deptAlias, String userAlias, String permission) {
        requireAlias(deptAlias);
        boolean hasUserAlias = userAlias != null && !userAlias.isBlank();
        if (hasUserAlias) {
            requireAlias(userAlias);
        }
        String[] requiredPerms = splitPermissions(permission);
        StringBuilder sql = new StringBuilder();
        List<String> conditions = new ArrayList<>();
        List<SysRole> roles = user.getRoles() == null ? List.of() : user.getRoles();

        for (SysRole role : roles) {
            String dataScope = role.getDataScope();
            if (!DATA_SCOPE_CUSTOM.equals(dataScope) && conditions.contains(dataScope)) {
                continue;
            }
            if (requiredPerms.length > 0 && role.getPermissions() != null && !role.getPermissions().isEmpty()
                    && !containsAny(role.getPermissions(), requiredPerms)) {
                continue;
            }
            if (DATA_SCOPE_ALL.equals(dataScope)) {
                sql.setLength(0);
                conditions.add(dataScope);
                break;
            } else if (DATA_SCOPE_CUSTOM.equals(dataScope)) {
                if (role.getRoleId() != null) {
                    sql.append(" OR ").append(deptAlias).append(".dept_id IN ( SELECT dept_id FROM sys_role_dept WHERE role_id = ")
                            .append(role.getRoleId().longValue()).append(" ) ");
                }
            } else if (DATA_SCOPE_DEPT.equals(dataScope)) {
                Long deptId = user.getDeptId();
                if (deptId == null) {
                    appendNothing(sql, deptAlias);
                } else {
                    sql.append(" OR ").append(deptAlias).append(".dept_id = ").append(deptId.longValue()).append(' ');
                }
            } else if (DATA_SCOPE_DEPT_AND_CHILD.equals(dataScope)) {
                Long deptId = user.getDeptId();
                if (deptId == null) {
                    appendNothing(sql, deptAlias);
                } else {
                    long id = deptId.longValue();
                    // find_in_set 在 H2 中不可用，改用逗号包边的 LIKE，两种库结果一致
                    sql.append(" OR ").append(deptAlias).append(".dept_id IN ( SELECT dept_id FROM sys_dept WHERE dept_id = ")
                            .append(id).append(" OR CONCAT(',', ancestors, ',') LIKE '%,").append(id).append(",%' ) ");
                }
            } else if (DATA_SCOPE_SELF.equals(dataScope)) {
                if (hasUserAlias && user.getUserId() != null) {
                    sql.append(" OR ").append(userAlias).append(".user_id = ").append(user.getUserId().longValue()).append(' ');
                } else {
                    appendNothing(sql, deptAlias);
                }
            }
            conditions.add(dataScope);
        }

        if (conditions.isEmpty()) {
            appendNothing(sql, deptAlias);
        }
        if (sql.toString().isBlank()) {
            return "";
        }
        return " AND (" + sql.substring(4) + ")";
    }

    private static void appendNothing(StringBuilder sql, String deptAlias) {
        sql.append(" OR ").append(deptAlias).append(".dept_id = 0 ");
    }

    private static boolean containsAny(Set<String> rolePerms, String[] requiredPerms) {
        if (rolePerms.contains(ALL_PERMISSION)) {
            return true;
        }
        for (String perm : requiredPerms) {
            if (rolePerms.contains(perm)) {
                return true;
            }
        }
        return false;
    }

    private static String[] splitPermissions(String permission) {
        if (permission == null || permission.isBlank()) {
            return new String[0];
        }
        return Arrays.stream(permission.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
    }

    private static void requireAlias(String alias) {
        if (alias == null || !SQL_ALIAS.matcher(alias).matches()) {
            throw new IllegalArgumentException("非法的 SQL 表别名: " + alias);
        }
    }
}
