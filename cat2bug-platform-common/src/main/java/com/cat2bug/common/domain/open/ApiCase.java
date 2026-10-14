package com.cat2bug.common.domain.open;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

/**
 * Open API 测试用例视图，对齐 Spring {@code com.cat2bug.api.domain.ApiCase}。
 */
public class ApiCase {

    private Long caseNum;
    private String caseName;
    private String caseExpect;
    private List<ApiCaseStep> caseStep;
    private String casePreconditions;
    private String caseData;
    @JsonIgnore
    private Long deliverableId;
    private String deliverableName;
    private String remark;
    private Long caseLevel;
    private List<String> imgUrlList;

    public Long getCaseNum() {
        return caseNum;
    }

    public void setCaseNum(Long caseNum) {
        this.caseNum = caseNum;
    }

    public String getCaseName() {
        return caseName;
    }

    public void setCaseName(String caseName) {
        this.caseName = caseName;
    }

    public String getCaseExpect() {
        return caseExpect;
    }

    public void setCaseExpect(String caseExpect) {
        this.caseExpect = caseExpect;
    }

    public List<ApiCaseStep> getCaseStep() {
        return caseStep;
    }

    public void setCaseStep(List<ApiCaseStep> caseStep) {
        this.caseStep = caseStep;
    }

    public String getCasePreconditions() {
        return casePreconditions;
    }

    public void setCasePreconditions(String casePreconditions) {
        this.casePreconditions = casePreconditions;
    }

    public String getCaseData() {
        return caseData;
    }

    public void setCaseData(String caseData) {
        this.caseData = caseData;
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

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Long getCaseLevel() {
        return caseLevel;
    }

    public void setCaseLevel(Long caseLevel) {
        this.caseLevel = caseLevel;
    }

    public List<String> getImgUrlList() {
        return imgUrlList;
    }

    public void setImgUrlList(List<String> imgUrlList) {
        this.imgUrlList = imgUrlList;
    }
}
