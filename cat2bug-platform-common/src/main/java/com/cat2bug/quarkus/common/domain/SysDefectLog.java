package com.cat2bug.quarkus.common.domain;

import com.cat2bug.quarkus.common.constant.DefectLogState;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;
import java.util.List;

public class SysDefectLog {

    private Long defectLogId;
    private String defectLogDescribe;
    private DefectLogState defectLogType;
    private List<Long> receiveBy;
    private String annexUrls;
    private Long defectId;
    private String createBy;
    private String createByName;
    private String createByAvatar;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    private String srcHost;

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
}
