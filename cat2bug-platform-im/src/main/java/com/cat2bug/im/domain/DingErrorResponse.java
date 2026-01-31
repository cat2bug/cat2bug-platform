package com.cat2bug.im.domain;

import lombok.Data;

/**
 * @Author: yuzhantao
 * @CreateTime: 2026-01-31 10:52
 * @Version: 1.0.0
 */
@Data
public class DingErrorResponse {
    /**
     * 无效鉴权
     */
    public final static String INVALID_AUTHENTICATION_CODE = "InvalidAuthentication";
    /**
     * 返回编码
     */
    private String code;
    /**
     * 返回请求ID
     */
    private String requestid;
    /**
     * 返回信息
     */
    private String message;
}
