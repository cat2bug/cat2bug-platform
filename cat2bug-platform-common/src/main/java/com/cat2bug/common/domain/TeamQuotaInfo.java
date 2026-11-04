package com.cat2bug.common.domain;

/**
 * 团队套餐与当前占用（新建页配额条）。
 */
public class TeamQuotaInfo {

    private Long teamId;
    private Long planId;
    private String planName;
    private int maxMembers;
    private int maxProjects;
    private long maxProjectBytes;
    private int memberCount;
    private int projectCount;
    private int memberRemaining;
    private int projectRemaining;

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public Long getPlanId() {
        return planId;
    }

    public void setPlanId(Long planId) {
        this.planId = planId;
    }

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }

    public int getMaxMembers() {
        return maxMembers;
    }

    public void setMaxMembers(int maxMembers) {
        this.maxMembers = maxMembers;
    }

    public int getMaxProjects() {
        return maxProjects;
    }

    public void setMaxProjects(int maxProjects) {
        this.maxProjects = maxProjects;
    }

    public long getMaxProjectBytes() {
        return maxProjectBytes;
    }

    public void setMaxProjectBytes(long maxProjectBytes) {
        this.maxProjectBytes = maxProjectBytes;
    }

    public int getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(int memberCount) {
        this.memberCount = memberCount;
    }

    public int getProjectCount() {
        return projectCount;
    }

    public void setProjectCount(int projectCount) {
        this.projectCount = projectCount;
    }

    public int getMemberRemaining() {
        return memberRemaining;
    }

    public void setMemberRemaining(int memberRemaining) {
        this.memberRemaining = memberRemaining;
    }

    public int getProjectRemaining() {
        return projectRemaining;
    }

    public void setProjectRemaining(int projectRemaining) {
        this.projectRemaining = projectRemaining;
    }
}
