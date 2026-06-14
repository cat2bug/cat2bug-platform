package com.cat2bug.web.vo;

import com.cat2bug.ai.annotaion.AIClass;
import com.cat2bug.ai.annotaion.AIField;
import com.cat2bug.system.domain.SysCaseStep;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.List;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-06-15 02:44
 * @Version: 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@AIClass
public class AiCase {
    /** 测试用例名称 */
    @AIField(explain = "测试用例名称,用于简短介绍测试用例用途", minValue = "5个字节", maxValue = "64个字节")
    private String caseName;

    /** 用例级别 */
    @AIField(explain = "用例级别", minValue = "1",maxValue = "5")
    private Long caseLevel;

    /** 前置条件 */
    @AIField(explain = "前置条件")
    private String casePreconditions;

    /** 预期 */
    @AIField(explain = "预期结果")
    private String caseExpect;

    /** 步骤 */
    @AIField(explain = "测试步骤数组", minValue = "2条步骤", maxValue = "10条步骤")
    private List<AiCaseStep> caseStep;
}
