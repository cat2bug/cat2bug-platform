package com.cat2bug.ai.vo;

import com.sun.org.apache.xpath.internal.operations.Bool;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-06-12 01:21
 * @Version: 1.0.0
 * Ollama模型请求对象
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OllamaModuleRequest {
    /**
     * 模型名称
     */
    private String name;
//    /**
//     * （可选）允许与库的不安全连接。仅当您在开发过程中从自己的库中提取时才使用此。
//     */
//    private boolean insecure;
    /**
     * （可选）如果false，响应将作为单个响应对象返回，而不是对象流
     */
    private Boolean stream;

    public OllamaModuleRequest(String name) {
        this.name = name;
    }
}
