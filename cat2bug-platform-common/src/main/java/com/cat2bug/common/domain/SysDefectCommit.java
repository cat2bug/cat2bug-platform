package com.cat2bug.common.domain;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;

/**
 * 缺陷修复关联的提交 sys_defect_commit。托管仓库填 {@code repositoryId}，外部仓库填 {@code commitUrl}。
 */
public class SysDefectCommit {

    private Long commitLinkId;
    private Long defectId;
    private Long defectLogId;
    private Long repositoryId;
    /** 托管仓库的展示名（查询时联表得到） */
    private String repositoryName;
    private String commitSha;
    private String commitUrl;
    private String commitMessage;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date commitTime;
    private String createBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    public Long getCommitLinkId() {
        return commitLinkId;
    }

    public void setCommitLinkId(Long commitLinkId) {
        this.commitLinkId = commitLinkId;
    }

    public Long getDefectId() {
        return defectId;
    }

    public void setDefectId(Long defectId) {
        this.defectId = defectId;
    }

    public Long getDefectLogId() {
        return defectLogId;
    }

    public void setDefectLogId(Long defectLogId) {
        this.defectLogId = defectLogId;
    }

    public Long getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(Long repositoryId) {
        this.repositoryId = repositoryId;
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    public void setRepositoryName(String repositoryName) {
        this.repositoryName = repositoryName;
    }

    public String getCommitSha() {
        return commitSha;
    }

    public void setCommitSha(String commitSha) {
        this.commitSha = commitSha;
    }

    public String getCommitUrl() {
        return commitUrl;
    }

    public void setCommitUrl(String commitUrl) {
        this.commitUrl = commitUrl;
    }

    public String getCommitMessage() {
        return commitMessage;
    }

    public void setCommitMessage(String commitMessage) {
        this.commitMessage = commitMessage;
    }

    public Date getCommitTime() {
        return commitTime;
    }

    public void setCommitTime(Date commitTime) {
        this.commitTime = commitTime;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}
