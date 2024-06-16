package com.cat2bug.web.vo;

import com.cat2bug.ai.annotaion.AIClass;
import com.cat2bug.ai.annotaion.AIField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-06-13 23:09
 * @Version: 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@AIClass
public class AiCaseStep {
    /**
     * 描述
     */
    @AIField(explain = "测试步骤描述", minValue = "5个字节", maxValue = "64个字节")
    private String stepDescribe;
    /**
     * 预期
     */
    @AIField(explain = "测试步骤预期效果", minValue = "5个字节", maxValue = "64个字节")
    private String stepExpect;
}
