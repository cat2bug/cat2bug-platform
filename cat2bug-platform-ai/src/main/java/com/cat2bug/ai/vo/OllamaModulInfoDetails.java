package com.cat2bug.ai.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-06-12 02:04
 * @Version: 1.0.0
 * 模型信息详情
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OllamaModulInfoDetails {
    private String format;
    private String family;
    private String[] families;
    private String parameter_size;
    private String quantization_level;
}
