package com.cat2bug.common.domain;

public class SysUserProjectRole {

    private Long userProjectRoleId;
    private Long userProjectId;
    private Long roleId;

    public Long getUserProjectRoleId() {
        return userProjectRoleId;
    }

    public void setUserProjectRoleId(Long userProjectRoleId) {
        this.userProjectRoleId = userProjectRoleId;
    }

    public Long getUserProjectId() {
        return userProjectId;
    }

    public void setUserProjectId(Long userProjectId) {
        this.userProjectId = userProjectId;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }
}
