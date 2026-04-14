package com.cat2bug.im.domain;

import lombok.Data;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-07-18 00:31
 * @Version: 1.0.0
 * 飞书在项目上的配置（自定义机器人 Webhook 方式）
 */
@Data
public class FeishuProjectConfig {
    public final static String SYSTEM_CODE = "feishu";

    /** Webhook 地址 */
    private String hook;

    /** 签名密钥（可选） */
    private String secret;
}
