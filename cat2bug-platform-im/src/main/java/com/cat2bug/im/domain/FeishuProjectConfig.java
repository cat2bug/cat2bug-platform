package com.cat2bug.im.domain;

import lombok.Data;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-07-18 00:31
 * @Version: 1.0.0
 * 飞书在项目上的配置（企业应用方式）
 */
@Data
public class FeishuProjectConfig {
    public final static String SYSTEM_CODE = "feishu";

    /** 应用凭证（App ID） */
    private String appId;

    /** 应用密钥（App Secret） */
    private String appSecret;
}
