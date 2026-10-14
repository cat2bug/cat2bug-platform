package com.cat2bug.admin;

import com.cat2bug.common.domain.SysRole;
import com.cat2bug.common.domain.SysUser;
import com.cat2bug.system.security.DataScopeSqlBuilder;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** 数据范围 SQL 片段：五种范围、多角色并集与权限字符筛选，期望值对照原 Spring 线 DataScopeAspect。 */
class DataScopeSqlBuilderTest {

    @Test
    void allScopeProducesNoFilter() {
        assertEquals("", build(user(role(1L, "1"))));
    }

    @Test
    void customScopeUsesRoleDeptTable() {
        assertEquals(" AND (d.dept_id IN ( SELECT dept_id FROM sys_role_dept WHERE role_id = 7 ) )",
                build(user(role(7L, "2"))));
    }

    @Test
    void deptScopeUsesOwnDept() {
        assertEquals(" AND (d.dept_id = 103 )", build(user(role(1L, "3"))));
    }

    @Test
    void deptAndChildScopeMatchesAncestorsPortably() {
        assertEquals(" AND (d.dept_id IN ( SELECT dept_id FROM sys_dept WHERE dept_id = 103"
                        + " OR CONCAT(',', ancestors, ',') LIKE '%,103,%' ) )",
                build(user(role(1L, "4"))));
    }

    @Test
    void selfScopeUsesUserAliasOrMatchesNothing() {
        SysUser u = user(role(1L, "5"));
        assertEquals(" AND (u.user_id = 42 )", DataScopeSqlBuilder.build(u, "d", "u", null));
        assertEquals(" AND (d.dept_id = 0 )", DataScopeSqlBuilder.build(u, "d", null, null));
    }

    @Test
    void multipleRolesAreUnioned() {
        assertEquals(" AND (d.dept_id = 103  OR u.user_id = 42 )", build(user(role(1L, "3"), role(2L, "5"))));
    }

    @Test
    void anyAllRoleWinsOverNarrowerRoles() {
        assertEquals("", build(user(role(1L, "3"), role(2L, "1"))));
    }

    @Test
    void customScopesOfDifferentRolesAreAllKept() {
        assertEquals(" AND (d.dept_id IN ( SELECT dept_id FROM sys_role_dept WHERE role_id = 7 )"
                        + "  OR d.dept_id IN ( SELECT dept_id FROM sys_role_dept WHERE role_id = 8 ) )",
                build(user(role(7L, "2"), role(8L, "2"))));
    }

    @Test
    void rolesWithoutRequiredPermissionAreSkipped() {
        SysRole wide = role(1L, "1");
        wide.setPermissions(Set.of("system:dept:list"));
        SysRole narrow = role(2L, "3");
        narrow.setPermissions(Set.of("system:user:list"));
        SysUser u = user(wide, narrow);

        assertEquals(" AND (d.dept_id = 103 )", DataScopeSqlBuilder.build(u, "d", "u", "system:user:list"));
        assertEquals("", DataScopeSqlBuilder.build(u, "d", "u", "system:dept:list"));
        assertEquals("", DataScopeSqlBuilder.build(u, "d", "u", "system:user:list,system:dept:list"));
    }

    @Test
    void wildcardPermissionMatchesAnyRequirement() {
        SysRole role = role(1L, "3");
        role.setPermissions(Set.of("*:*:*"));
        assertEquals(" AND (d.dept_id = 103 )",
                DataScopeSqlBuilder.build(user(role), "d", "u", "system:user:list"));
    }

    @Test
    void noApplicableRoleMatchesNothing() {
        SysRole role = role(1L, "1");
        role.setPermissions(Set.of("system:dept:list"));
        assertEquals(" AND (d.dept_id = 0 )", DataScopeSqlBuilder.build(user(role), "d", "u", "system:user:list"));
        assertEquals(" AND (d.dept_id = 0 )", build(user()));
    }

    @Test
    void missingDeptMatchesNothingInsteadOfEveryRootDept() {
        SysUser u = user(role(1L, "4"));
        u.setDeptId(null);
        assertEquals(" AND (d.dept_id = 0 )", build(u));
    }

    @Test
    void rejectsNonIdentifierAlias() {
        SysUser u = user(role(1L, "3"));
        assertThrows(IllegalArgumentException.class, () -> DataScopeSqlBuilder.build(u, "d;drop", "u", null));
        assertThrows(IllegalArgumentException.class, () -> DataScopeSqlBuilder.build(u, "d", "u or 1=1", null));
    }

    private static String build(SysUser user) {
        return DataScopeSqlBuilder.build(user, "d", "u", null);
    }

    private static SysUser user(SysRole... roles) {
        SysUser user = new SysUser();
        user.setUserId(42L);
        user.setDeptId(103L);
        user.setRoles(new ArrayList<>(List.of(roles)));
        return user;
    }

    private static SysRole role(Long roleId, String dataScope) {
        SysRole role = new SysRole(roleId);
        role.setDataScope(dataScope);
        return role;
    }
}
