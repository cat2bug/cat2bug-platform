package com.cat2bug.ai.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-06-22 16:17
 * @Version: 1.0.0
 */
@Data
public class OllamaPullModuleResponse extends AiPullModuleResponse {
    private String status;
    private String digest;
}
