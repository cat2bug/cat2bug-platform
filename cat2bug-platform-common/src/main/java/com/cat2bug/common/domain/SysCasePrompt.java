package com.cat2bug.common.domain;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;

public class SysCasePrompt {

    private String casePromptId;
    private String casePromptContent;
    private Long projectId;
    private Long createById;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    public String getCasePromptId() {
        return casePromptId;
    }

    public void setCasePromptId(String casePromptId) {
        this.casePromptId = casePromptId;
    }

    public String getCasePromptContent() {
        return casePromptContent;
    }

    public void setCasePromptContent(String casePromptContent) {
        this.casePromptContent = casePromptContent;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getCreateById() {
        return createById;
    }

    public void setCreateById(Long createById) {
        this.createById = createById;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}
