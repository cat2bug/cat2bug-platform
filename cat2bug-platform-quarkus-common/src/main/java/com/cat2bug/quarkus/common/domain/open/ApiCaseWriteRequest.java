package com.cat2bug.quarkus.common.domain.open;

import java.util.List;

/**
 * Open API 创建/更新测试用例请求体，对齐 Spring {@code com.cat2bug.api.domain.ApiCaseWriteRequest}。
 */
public class ApiCaseWriteRequest {

    /** 更新时必填：项目内用例编号 */
    private Long caseNum;
    private String caseName;
    private String caseExpect;
    private List<ApiCaseStep> caseStep;
    private Long caseLevel;
    private String casePreconditions;
    private String caseData;
    /** 交付物全路径，多级用 {@code /} 分隔 */
    private String deliverableName;
    private String remark;

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

    public Long getCaseLevel() {
        return caseLevel;
    }

    public void setCaseLevel(Long caseLevel) {
        this.caseLevel = caseLevel;
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
}
