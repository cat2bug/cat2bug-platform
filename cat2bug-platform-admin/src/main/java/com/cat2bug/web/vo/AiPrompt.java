package com.cat2bug.web.vo;

import lombok.Data;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-06-16 16:51
 * @Version: 1.0.0
 */
@Data
public class AiPrompt {
    private String prompt;
    private int rowCount;
    private long[] context;
}
