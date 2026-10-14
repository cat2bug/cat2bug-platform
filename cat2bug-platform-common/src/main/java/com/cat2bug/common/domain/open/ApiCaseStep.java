package com.cat2bug.common.domain.open;

/**
 * Open API 用例步骤，对齐 Spring {@code com.cat2bug.api.domain.ApiCaseStep}。
 */
public class ApiCaseStep {

    private String stepDescribe;
    private String stepExpect;

    public String getStepDescribe() {
        return stepDescribe;
    }

    public void setStepDescribe(String stepDescribe) {
        this.stepDescribe = stepDescribe;
    }

    public String getStepExpect() {
        return stepExpect;
    }

    public void setStepExpect(String stepExpect) {
        this.stepExpect = stepExpect;
    }
}
