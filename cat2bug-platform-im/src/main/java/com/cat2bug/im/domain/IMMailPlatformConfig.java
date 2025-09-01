package com.cat2bug.im.domain;

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
public class IMMailPlatformConfig extends IMBasePlatformConfig {

    /**
     * 接收人的邮箱
     */
    private String receiver;

    public IMMailPlatformConfig(boolean configSwitch, String receiver) {
        super(configSwitch);
        this.receiver = receiver;
    }
}
