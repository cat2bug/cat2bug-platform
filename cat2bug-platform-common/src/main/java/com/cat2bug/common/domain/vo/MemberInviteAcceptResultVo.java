package com.cat2bug.common.domain.vo;

/**
 * 接受邀请结果
 */
public class MemberInviteAcceptResultVo {

    private String scope;
    private Long teamId;
    private Long projectId;
    private Long repositoryId;
    private String redirectPath;
    private Boolean alreadyMember;
    private String message;

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(Long repositoryId) {
        this.repositoryId = repositoryId;
    }

    public String getRedirectPath() {
        return redirectPath;
    }

    public void setRedirectPath(String redirectPath) {
        this.redirectPath = redirectPath;
    }

    public Boolean getAlreadyMember() {
        return alreadyMember;
    }

    public void setAlreadyMember(Boolean alreadyMember) {
        this.alreadyMember = alreadyMember;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
