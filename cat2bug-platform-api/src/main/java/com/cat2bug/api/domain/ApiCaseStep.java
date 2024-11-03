package com.cat2bug.api.domain;

import lombok.Data;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-01-28 00:22
 * @Version: 1.0.0
 */
@Data
public class ApiCaseStep {
    /**
     * 描述
     */
    private String stepDescribe;
    /**
     * 预期
     */
    private String stepExpect;
}
