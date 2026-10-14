package com.cat2bug.common.domain.open;

import com.cat2bug.common.domain.open.type.ApiDefectLogStateEnum;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Date;
import java.util.List;

/**
 * Open API 缺陷日志，对齐 Spring {@code com.cat2bug.api.domain.ApiDefectLog}。
 */
public class ApiDefectLog {

    @JsonIgnore
    private Long defectLogId;
    @JsonIgnore
    private Long defectId;
    private ApiDefectLogStateEnum defectLogState;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    @JsonIgnore
    private Long createById;
    @JsonIgnore
    private String createBy;
    @JsonIgnore
    private List<ApiMember> receiverList;
    @JsonIgnore
    private List<Long> receiveBy;
    private String defectLogDescribe;

    public Long getDefectLogId() {
        return defectLogId;
    }

    public void setDefectLogId(Long defectLogId) {
        this.defectLogId = defectLogId;
    }

    public Long getDefectId() {
        return defectId;
    }

    public void setDefectId(Long defectId) {
        this.defectId = defectId;
    }

    public ApiDefectLogStateEnum getDefectLogState() {
        return defectLogState;
    }

    public void setDefectLogState(ApiDefectLogStateEnum defectLogState) {
        this.defectLogState = defectLogState;
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

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public List<ApiMember> getReceiverList() {
        return receiverList;
    }

    public void setReceiverList(List<ApiMember> receiverList) {
        this.receiverList = receiverList;
    }

    public List<Long> getReceiveBy() {
        return receiveBy;
    }

    public void setReceiveBy(List<Long> receiveBy) {
        this.receiveBy = receiveBy;
    }

    public String getDefectLogDescribe() {
        return defectLogDescribe;
    }

    public void setDefectLogDescribe(String defectLogDescribe) {
        this.defectLogDescribe = defectLogDescribe;
    }
}
