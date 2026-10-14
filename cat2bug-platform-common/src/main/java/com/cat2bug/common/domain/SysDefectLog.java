package com.cat2bug.common.domain;

import com.cat2bug.common.constant.DefectLogState;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;
import java.util.List;

public class SysDefectLog {

    private Long defectLogId;
    private String defectLogDescribe;
    private DefectLogState defectLogType;
    private List<Long> receiveBy;
    private List<SysUser> receiveByList;
    private String annexUrls;
    private Long defectId;
    private String createBy;
    private String createByName;
    private String createByAvatar;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    private String srcHost;
    private List<SysComment> commentList;
    /** 来源渠道：{@code web} / {@code mcp} / {@code api}；为空视为网页 */
    private String operChannel;
    /** 经 MCP 时客户端在 initialize 中上报的名称 */
    private String clientName;
    /** 修复时关联的提交 */
    private List<SysDefectCommit> commits;

    public Long getDefectLogId() {
        return defectLogId;
    }

    public void setDefectLogId(Long defectLogId) {
        this.defectLogId = defectLogId;
    }

    public String getDefectLogDescribe() {
        return defectLogDescribe;
    }

    public void setDefectLogDescribe(String defectLogDescribe) {
        this.defectLogDescribe = defectLogDescribe;
    }

    public DefectLogState getDefectLogType() {
        return defectLogType;
    }

    public void setDefectLogType(DefectLogState defectLogType) {
        this.defectLogType = defectLogType;
    }

    public List<Long> getReceiveBy() {
        return receiveBy;
    }

    public void setReceiveBy(List<Long> receiveBy) {
        this.receiveBy = receiveBy;
    }

    public String getAnnexUrls() {
        return annexUrls;
    }

    public void setAnnexUrls(String annexUrls) {
        this.annexUrls = annexUrls;
    }

    public Long getDefectId() {
        return defectId;
    }

    public void setDefectId(Long defectId) {
        this.defectId = defectId;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public String getCreateByName() {
        return createByName;
    }

    public void setCreateByName(String createByName) {
        this.createByName = createByName;
    }

    public String getCreateByAvatar() {
        return createByAvatar;
    }

    public void setCreateByAvatar(String createByAvatar) {
        this.createByAvatar = createByAvatar;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getSrcHost() {
        return srcHost;
    }

    public void setSrcHost(String srcHost) {
        this.srcHost = srcHost;
    }

    public List<SysUser> getReceiveByList() {
        return receiveByList;
    }

    public void setReceiveByList(List<SysUser> receiveByList) {
        this.receiveByList = receiveByList;
    }

    public List<SysComment> getCommentList() {
        return commentList;
    }

    public void setCommentList(List<SysComment> commentList) {
        this.commentList = commentList;
    }

    public String getOperChannel() {
        return operChannel;
    }

    public void setOperChannel(String operChannel) {
        this.operChannel = operChannel;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public List<SysDefectCommit> getCommits() {
        return commits;
    }

    public void setCommits(List<SysDefectCommit> commits) {
        this.commits = commits;
    }
}
