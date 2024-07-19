package com.cat2bug.im.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-07-18 00:29
 * @Version: 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class IMBasePlatformConfig {
    /**
     * 通知开关
     */
    @JsonProperty("switch")
    private boolean configSwitch;
}
