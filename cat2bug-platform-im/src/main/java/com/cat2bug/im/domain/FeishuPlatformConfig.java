package com.cat2bug.im.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @Author: yuzhantao
 * @CreateTime: 2026-04-16
 * @Version: 1.0.0
 * 飞书用户级配置
 */
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Data
public class FeishuPlatformConfig extends IMBasePlatformConfig {
    /** 单发开关 */
    private Boolean singleSwitch;
    /** 群发开关 */
    private Boolean groupSwitch;
    /** 飞书用户手机号（用于接收个人消息，通过手机号查询 user_id） */
    private String mobile;
    /** 群发关键词（用于群机器人） */
    private String key;
    /** 签名密钥（用于群机器人签名校验） */
    private String secret;
    /** 群发 Hook 地址（用于群机器人） */
    private String hook;

    public FeishuPlatformConfig(boolean configSwitch, Boolean singleSwitch, Boolean groupSwitch, String mobile, String key, String secret, String hook) {
        super(configSwitch);
        this.singleSwitch = singleSwitch;
        this.groupSwitch = groupSwitch;
        this.mobile = mobile;
        this.key = key;
        this.secret = secret;
        this.hook = hook;
    }
}
