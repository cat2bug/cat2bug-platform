package com.cat2bug.ai.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-06-12 02:15
 * @Version: 1.0.0
 * 模型列表项
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OllamaModuleListItem extends AiModule {
    private String modified_at;
    private String digest;
    private OllamaModulInfoDetails details;
}
