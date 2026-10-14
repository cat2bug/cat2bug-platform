package com.cat2bug.common.exception;

/**
 * 微信小程序未绑定系统用户。
 */
public class WechatMpNotBindException extends ApiException {

    private final String sessionKey;

    public WechatMpNotBindException(String sessionKey) {
        super(40001, "微信小程序没有绑定用户");
        this.sessionKey = sessionKey;
    }

    public String getSessionKey() {
        return sessionKey;
    }
}
