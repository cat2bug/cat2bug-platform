package com.cat2bug.quarkus.common.domain.vo.ai;

import com.cat2bug.quarkus.common.ai.AIClass;
import com.cat2bug.quarkus.common.ai.AIField;

@AIClass
public class AiCaseStep {

    @AIField(explain = "测试步骤描述", minValue = "5个字节", maxValue = "64个字节")
    private String stepDescribe;

    @AIField(explain = "测试步骤预期效果", minValue = "5个字节", maxValue = "64个字节")
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
