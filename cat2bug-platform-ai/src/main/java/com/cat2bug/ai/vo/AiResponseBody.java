package com.cat2bug.ai.vo;

import lombok.Data;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-06-16 16:10
 * @Version: 1.0.0
 */
@Data
public class AiResponseBody {
    /**
     * 请求上下文
     */
    private long[] context;
}
