package com.cat2bug.common.domain.vo.ai;

import com.cat2bug.common.ai.AIClass;
import com.cat2bug.common.ai.AIField;

import java.util.List;

@AIClass
public class AiCase {

    @AIField(explain = "测试用例名称,用于简短介绍测试用例用途", minValue = "5个字节", maxValue = "64个字节")
    private String caseName;

    @AIField(explain = "用例级别", minValue = "1", maxValue = "5")
    private Long caseLevel;

    @AIField(explain = "前置条件")
    private String casePreconditions;

    @AIField(explain = "预期结果")
    private String caseExpect;

    @AIField(explain = "测试步骤数组", minValue = "2条步骤", maxValue = "10条步骤")
    private List<AiCaseStep> caseStep;

    public String getCaseName() {
        return caseName;
    }

    public void setCaseName(String caseName) {
        this.caseName = caseName;
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

    public String getCaseExpect() {
        return caseExpect;
    }

    public void setCaseExpect(String caseExpect) {
        this.caseExpect = caseExpect;
    }

    public List<AiCaseStep> getCaseStep() {
        return caseStep;
    }

    public void setCaseStep(List<AiCaseStep> caseStep) {
        this.caseStep = caseStep;
    }
}
