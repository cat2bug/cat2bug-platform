package com.cat2bug.im.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-07-26 16:57
 * @Version: 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class EnterpriseWeChatPlatformConfig extends IMBasePlatformConfig {
    /** 单发开关 */
    private Boolean singleSwitch;
    /** 群发开关 */
    private Boolean groupSwitch;
    /** 企业微信手机号（用于单发，通过手机号查询 userid） */
    private String mobile;
    /** 企业微信群机器人 Webhook（用于群发） */
    private String hook;
}
