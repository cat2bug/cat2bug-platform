package com.cat2bug.common.domain.open;

import com.cat2bug.common.domain.open.type.ApiDefectStateEnum;
import com.cat2bug.common.domain.open.type.ApiDefectTypeEnum;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Date;
import java.util.List;

/**
 * Open API 缺陷视图，对齐 Spring {@code com.cat2bug.api.domain.ApiDefect}。
 */
public class ApiDefect {

    @JsonIgnore
    private Long defectId;
    private Long defectNum;
    private String defectName;
    private ApiDefectTypeEnum defectType;
    private ApiDefectStateEnum defectState;
    private String defectLevel;
    @JsonIgnore
    private Long deliverableId;
    private String deliverableName;
    private String version;
    private ApiMemberBaseInfo creator;
    private List<ApiMemberBaseInfo> handlerList;
    private List<String> imgUrlList;
    private List<String> annexUrlList;
    private String defectKey;
    private String defectGroupKey;
    private String defectDescribe;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
    private ApiDefectLog currentLog;
    @JsonIgnore
    private Long updateById;
    @JsonIgnore
    private String updateBy;
    @JsonIgnore
    private Long createById;
    @JsonIgnore
    private String createBy;
    @JsonIgnore
    private Date createTime;
    @JsonIgnore
    private Long projectId;

    public Long getDefectId() {
        return defectId;
    }

    public void setDefectId(Long defectId) {
        this.defectId = defectId;
    }

    public Long getDefectNum() {
        return defectNum;
    }

    public void setDefectNum(Long defectNum) {
        this.defectNum = defectNum;
    }

    public String getDefectName() {
        return defectName;
    }

    public void setDefectName(String defectName) {
        this.defectName = defectName;
    }

    public ApiDefectTypeEnum getDefectType() {
        return defectType;
    }

    public void setDefectType(ApiDefectTypeEnum defectType) {
        this.defectType = defectType;
    }

    public ApiDefectStateEnum getDefectState() {
        return defectState;
    }

    public void setDefectState(ApiDefectStateEnum defectState) {
        this.defectState = defectState;
    }

    public String getDefectLevel() {
        return defectLevel;
    }

    public void setDefectLevel(String defectLevel) {
        this.defectLevel = defectLevel;
    }

    public Long getDeliverableId() {
        return deliverableId;
    }

    public void setDeliverableId(Long deliverableId) {
        this.deliverableId = deliverableId;
    }

    public String getDeliverableName() {
        return deliverableName;
    }

    public void setDeliverableName(String deliverableName) {
        this.deliverableName = deliverableName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public ApiMemberBaseInfo getCreator() {
        return creator;
    }

    public void setCreator(ApiMemberBaseInfo creator) {
        this.creator = creator;
    }

    public List<ApiMemberBaseInfo> getHandlerList() {
        return handlerList;
    }

    public void setHandlerList(List<ApiMemberBaseInfo> handlerList) {
        this.handlerList = handlerList;
    }

    public List<String> getImgUrlList() {
        return imgUrlList;
    }

    public void setImgUrlList(List<String> imgUrlList) {
        this.imgUrlList = imgUrlList;
    }

    public List<String> getAnnexUrlList() {
        return annexUrlList;
    }

    public void setAnnexUrlList(List<String> annexUrlList) {
        this.annexUrlList = annexUrlList;
    }

    public String getDefectKey() {
        return defectKey;
    }

    public void setDefectKey(String defectKey) {
        this.defectKey = defectKey;
    }

    public String getDefectGroupKey() {
        return defectGroupKey;
    }

    public void setDefectGroupKey(String defectGroupKey) {
        this.defectGroupKey = defectGroupKey;
    }

    public String getDefectDescribe() {
        return defectDescribe;
    }

    public void setDefectDescribe(String defectDescribe) {
        this.defectDescribe = defectDescribe;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public ApiDefectLog getCurrentLog() {
        return currentLog;
    }

    public void setCurrentLog(ApiDefectLog currentLog) {
        this.currentLog = currentLog;
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

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }
}
