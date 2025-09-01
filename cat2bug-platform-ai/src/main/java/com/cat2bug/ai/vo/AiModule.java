package com.cat2bug.ai.vo;

import lombok.Data;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-06-21 13:34
 * @Version: 1.0.0
 */
@Data
public class AiModule {
    /** 模型名称 */
    private String name;
    /** 模型文件大小 */
    private long size;
    /** 模型状态 */
    private AiModuleStateEnum state;
}
