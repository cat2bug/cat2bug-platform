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
    /** 企业微信的用户ID */
    private String userId;
}
