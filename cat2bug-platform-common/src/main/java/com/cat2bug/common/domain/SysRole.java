package com.cat2bug.common.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Set;

public class SysRole {

    private Long roleId;
    private String roleName;
    /** 角色名称国际化 key */
    private String roleNameI18nKey;
    private String roleKey;
    private Integer roleSort;
    private String dataScope;
    private String status;
    private String remark;
    private boolean menuCheckStrictly;
    private boolean deptCheckStrictly;
    private boolean flag;
    private Boolean isTeamRole;
    private Boolean isProjectRole;
    private Long[] menuIds;
    private Long[] deptIds;
    /** 该角色的菜单权限字符，供数据范围按权限字符筛选角色 */
    private Set<String> permissions;
    private String createBy;
    private String updateBy;

    public SysRole() {
    }

    public SysRole(Long roleId) {
        this.roleId = roleId;
    }

    public boolean isAdmin() {
        return isAdmin(roleId);
    }

    public static boolean isAdmin(Long roleId) {
        return roleId != null && roleId == 1L;
    }

    /**
     * 是否项目管理员。Jackson 序列化为 {@code projectAdmin}，供成员页筛选项使用。
     * 与种子角色 id=6 / roleKey=project.admin 对齐。
     */
    public boolean isProjectAdmin() {
        return "project.admin".equals(roleKey) || (roleId != null && roleId == 6L);
    }

    /**
     * 是否项目创建人。Jackson 序列化为 {@code projectCreateBy}。
     */
    public boolean isProjectCreateBy() {
        return "project.create-by".equals(roleKey) || (roleId != null && roleId == 11L);
    }

    /**
     * 是否团队创建人。Jackson 序列化为 {@code teamCreateBy}。
     */
    public boolean isTeamCreateBy() {
        return "team.create-by".equals(roleKey) || (roleId != null && roleId == 12L);
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getRoleNameI18nKey() {
        return roleNameI18nKey;
    }

    public void setRoleNameI18nKey(String roleNameI18nKey) {
        this.roleNameI18nKey = roleNameI18nKey;
    }

    public String getRoleKey() {
        return roleKey;
    }

    public void setRoleKey(String roleKey) {
        this.roleKey = roleKey;
    }

    public Integer getRoleSort() {
        return roleSort;
    }

    public void setRoleSort(Integer roleSort) {
        this.roleSort = roleSort;
    }

    public String getDataScope() {
        return dataScope;
    }

    public void setDataScope(String dataScope) {
        this.dataScope = dataScope;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public boolean isMenuCheckStrictly() {
        return menuCheckStrictly;
    }

    public void setMenuCheckStrictly(boolean menuCheckStrictly) {
        this.menuCheckStrictly = menuCheckStrictly;
    }

    public boolean isDeptCheckStrictly() {
        return deptCheckStrictly;
    }

    public void setDeptCheckStrictly(boolean deptCheckStrictly) {
        this.deptCheckStrictly = deptCheckStrictly;
    }

    public boolean isFlag() {
        return flag;
    }

    public void setFlag(boolean flag) {
        this.flag = flag;
    }

    public Boolean getIsTeamRole() {
        return isTeamRole;
    }

    public void setIsTeamRole(Boolean isTeamRole) {
        this.isTeamRole = isTeamRole;
    }

    public Boolean getIsProjectRole() {
        return isProjectRole;
    }

    public void setIsProjectRole(Boolean isProjectRole) {
        this.isProjectRole = isProjectRole;
    }

    public Long[] getMenuIds() {
        return menuIds;
    }

    public void setMenuIds(Long[] menuIds) {
        this.menuIds = menuIds;
    }

    public Long[] getDeptIds() {
        return deptIds;
    }

    public void setDeptIds(Long[] deptIds) {
        this.deptIds = deptIds;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<String> permissions) {
        this.permissions = permissions;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }
}
