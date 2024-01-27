package com.cat2bug.system.domain;

import lombok.Data;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-01-28 00:22
 * @Version: 1.0.0
 */
@Data
public class SysCaseStep {
    /**
     * 描述
     */
    private String describe;
    /**
     * 预期
     */
    private String case_expect;
}
