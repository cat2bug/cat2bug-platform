package com.cat2bug.web.vo;

import lombok.Data;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-06-16 16:51
 * @Version: 1.0.0
 */
@Data
public class AiPrompt {
    /**
     * 服务类型，目前有openai、ollama
     */
    private String serviceType;
    /**
     * 模型ID
     */
    private Long modelId;
    /**
     * 提示词
     */
    private String prompt;
    /**
     * 需要结果的行数
     */
    private int rowCount;
    /**
     * 历史对话索引
     */
    private long[] context;
}
