package com.cat2bug.im.domain;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-07-18 00:30
 * @Version: 1.0.0
 * 邮件配置类
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class IMMailPlatformConfig {
    /**
     * 邮件通知开关
     */
    @JsonProperty("switch")
    private boolean configSwitch;
    /**
     * 发送人的邮箱
     */
    private String sender;
}
