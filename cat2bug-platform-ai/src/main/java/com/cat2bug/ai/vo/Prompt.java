package com.cat2bug.ai.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-06-11 12:05
 * @Version: 1.0.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Prompt {
    private String model;

    private String prompt;

    private String format;

    private boolean stream;
    /** 响应上下文 */
    private long[] context;

    public Prompt(String model, String prompt) {
        this(model,prompt,null);
    }

    public Prompt(String model, String prompt, String format) {
        this.model=model;
        this.prompt = prompt;
        this.format = format;
    }
}
