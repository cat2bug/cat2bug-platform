package com.cat2bug.web.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-06-25 23:09
 * @Version: 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class AiSysPrompt extends AiPrompt {
    private Long projectId;
}
