package com.cat2bug.system.exception;

import lombok.Getter;

/**
 * @Author: yuzhantao
 * @CreateTime: 2025-12-21 22:04
 * @Version: 1.0.0
 * 微信小程序没有绑定用户异常
 */
@Getter
public class WechatMpNotBindException extends AjaxResultException {
    private String sessionKey;

    private static int HTTP_CODE = 40001;

    public WechatMpNotBindException(String sessionKey) {
        super(HTTP_CODE, "微信小程序没有绑定用户");
        this.sessionKey = sessionKey;
    }
}
