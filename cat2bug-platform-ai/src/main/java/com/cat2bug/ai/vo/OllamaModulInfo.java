package com.cat2bug.ai.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-06-12 02:02
 * @Version: 1.0.0
 * Ollama模型信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OllamaModulInfo {
    private String modelfile;
    private String parameters;
    private String template;
    private OllamaModulInfoDetails details;
}
