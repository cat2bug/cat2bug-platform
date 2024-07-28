package com.cat2bug.im.domain;

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
public class IMDingPlatformConfig extends IMBasePlatformConfig {
    /**
     * 关键词
     */
    private String key;
    /**
     * 钩子函数
     */
    private String hook;
    /**
     * 用户ID
     */
    private String userId;

    public IMDingPlatformConfig(boolean configSwitch, String key, String hook, String userId) {
        super(configSwitch);
        this.key = key;
        this.hook = hook;
    }
}
