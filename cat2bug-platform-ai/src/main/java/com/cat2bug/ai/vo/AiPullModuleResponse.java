package com.cat2bug.ai.vo;

import lombok.Data;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-06-22 16:17
 * @Version: 1.0.0
 */
@Data
public class AiPullModuleResponse {
    private String name;
    private AiPullModuleStateEnum state;
    private long total;
    private long completed;
    private String layer;
    private String error;
}
