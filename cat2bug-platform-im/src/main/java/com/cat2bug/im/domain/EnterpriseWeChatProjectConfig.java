package com.cat2bug.im.domain;

import lombok.Data;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-07-27 11:29
 * @Version: 1.0.0
 */
@Data
public class EnterpriseWeChatProjectConfig {
    public final static String SYSTEM_CODE = "wechat";

    /** 企业ID */
    private String corpId;
    /** 应用ID */
    private String agentid;
    /** 应用的凭证密钥 */
    private String corpSecret;
}
