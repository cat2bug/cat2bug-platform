package com.cat2bug.im.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-07-18 00:31
 * @Version: 1.0.0
 * 钉钉配置
 */
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class IMDingPlatformConfig extends IMBasePlatformConfig {
    /** 单发开关 */
    private Boolean singleSwitch;
    /** 群发开关 */
    private Boolean groupSwitch;
    /** 关键词 */
    private String key;
    /** 加签密钥 */
    private String secret;
    /** 钩子函数 */
    private String hook;
    /** 手机号 */
    private String mobile;

    public IMDingPlatformConfig(boolean configSwitch, Boolean singleSwitch, Boolean groupSwitch, String key, String secret, String hook, String mobile) {
        super(configSwitch);
        this.singleSwitch = singleSwitch;
        this.groupSwitch = groupSwitch;
        this.key = key;
        this.secret = secret;
        this.hook = hook;
        this.mobile = mobile;
    }
}
