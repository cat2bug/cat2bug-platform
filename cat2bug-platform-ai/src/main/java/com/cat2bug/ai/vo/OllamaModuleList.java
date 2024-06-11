package com.cat2bug.ai.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-06-12 02:15
 * @Version: 1.0.0
 * 模型列表
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OllamaModuleList {
    private List<OllamaModuleListItem> models;
}
