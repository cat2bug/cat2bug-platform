package com.cat2bug.im.domain;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-07-18 00:31
 * @Version: 1.0.0
 * 钉钉配置
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class IMDingPlatformConfig {
    /**
     * 钉钉通知开关
     */
    @JsonProperty("switch")
    private boolean configSwitch;
    /**
     * 关键词
     */
    private String key;
    /**
     * 钩子函数
     */
    private String hook;
}
