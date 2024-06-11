package com.cat2bug.ai.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-06-12 01:29
 * @Version: 1.0.0
 * Ollama模型反馈
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OllamaModuleResponse {
    private String status;
    private String digest;
    private long total;
    private long completed;
}
