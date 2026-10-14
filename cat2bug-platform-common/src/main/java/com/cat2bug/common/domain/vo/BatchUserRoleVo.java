package com.cat2bug.common.domain.vo;

public class BatchUserRoleVo {

    private Long projectId;
    private Long teamId;
    private Long[] roleIds;
    private Long[] memberIds;

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public Long[] getRoleIds() {
        return roleIds;
    }

    public void setRoleIds(Long[] roleIds) {
        this.roleIds = roleIds;
    }

    public Long[] getMemberIds() {
        return memberIds;
    }

    public void setMemberIds(Long[] memberIds) {
        this.memberIds = memberIds;
    }
}
