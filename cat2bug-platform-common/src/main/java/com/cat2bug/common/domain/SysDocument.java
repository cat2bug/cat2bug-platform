package com.cat2bug.common.domain;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;

public class SysDocument {

    private Long docId;
    private Long projectId;
    private String docName;
    private Long docType;
    private String fileExtension;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    private Long createById;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
    private Long updateById;
    private String updateBy;
    private String updateByAvatar;
    private Long fileVersion;
    private Long docPid;
    private String docRemakr;
    private String fileUrl;
    private boolean leaf;
    /** 精确匹配文档名（查重），非表字段 */
    private String exactDocName;

    public Long getDocId() {
        return docId;
    }

    public void setDocId(Long docId) {
        this.docId = docId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getDocName() {
        return docName;
    }

    public void setDocName(String docName) {
        this.docName = docName;
    }

    public Long getDocType() {
        return docType;
    }

    public void setDocType(Long docType) {
        this.docType = docType;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public void setFileExtension(String fileExtension) {
        this.fileExtension = fileExtension;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Long getCreateById() {
        return createById;
    }

    public void setCreateById(Long createById) {
        this.createById = createById;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public Long getUpdateById() {
        return updateById;
    }

    public void setUpdateById(Long updateById) {
        this.updateById = updateById;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public String getUpdateByAvatar() {
        return updateByAvatar;
    }

    public void setUpdateByAvatar(String updateByAvatar) {
        this.updateByAvatar = updateByAvatar;
    }

    public Long getFileVersion() {
        return fileVersion;
    }

    public void setFileVersion(Long fileVersion) {
        this.fileVersion = fileVersion;
    }

    public Long getDocPid() {
        return docPid;
    }

    public void setDocPid(Long docPid) {
        this.docPid = docPid;
    }

    public String getDocRemakr() {
        return docRemakr;
    }

    public void setDocRemakr(String docRemakr) {
        this.docRemakr = docRemakr;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public boolean isLeaf() {
        return leaf;
    }

    public void setLeaf(boolean leaf) {
        this.leaf = leaf;
    }

    public String getExactDocName() {
        return exactDocName;
    }

    public void setExactDocName(String exactDocName) {
        this.exactDocName = exactDocName;
    }
}
