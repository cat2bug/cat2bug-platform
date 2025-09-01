package com.cat2bug.im.domain;

import lombok.Data;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-07-27 11:29
 * @Version: 1.0.0
 * 钉钉在项目上的配置
 */
@Data
public class DingProjectConfig {
    public final static String SYSTEM_CODE = "ding";

    /** 应用ID */
    private String appKey;
    /** 应用的凭证密钥 */
    private String appSecret;
    /** 机器人ID */
    private String robotCode;
}
