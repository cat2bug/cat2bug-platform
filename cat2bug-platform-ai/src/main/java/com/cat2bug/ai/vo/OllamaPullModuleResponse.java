package com.cat2bug.ai.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-06-22 16:17
 * @Version: 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class OllamaPullModuleResponse extends AiPullModuleResponse {
    private String status;
    private String digest;
}
