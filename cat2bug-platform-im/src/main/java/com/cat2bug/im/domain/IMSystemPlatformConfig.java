package com.cat2bug.im.domain;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-07-18 00:22
 * @Version: 1.0.0
 * 系统内部通知配置
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class IMSystemPlatformConfig {
    /**
     * 系统内部通知开关
     */
    @JsonProperty("switch")
    private boolean configSwitch;
    /**
     * 背景音乐文件名
     */
    private boolean backgroundMusic;
    /**
     * 是否显示提示面板
     */
    private boolean panel;
}
