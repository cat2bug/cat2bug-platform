package com.cat2bug.im.domain;

import lombok.Data;

/**
 * 飞书 tenant access token 返回结果
 */
@Data
public class FeishuTokenResult {
    private int code;
    private String msg;
    private String tenant_access_token;
    private int expire;
}
