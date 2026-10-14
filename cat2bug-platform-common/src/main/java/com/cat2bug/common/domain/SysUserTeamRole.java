package com.cat2bug.common.domain;

public class SysUserTeamRole {

    private Long userTeamRoleId;
    private Long userTeamId;
    private Long roleId;

    public Long getUserTeamRoleId() {
        return userTeamRoleId;
    }

    public void setUserTeamRoleId(Long userTeamRoleId) {
        this.userTeamRoleId = userTeamRoleId;
    }

    public Long getUserTeamId() {
        return userTeamId;
    }

    public void setUserTeamId(Long userTeamId) {
        this.userTeamId = userTeamId;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }
}
