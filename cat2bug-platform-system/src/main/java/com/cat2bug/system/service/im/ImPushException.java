package com.cat2bug.system.service.im;

/**
 * 外部 IM 平台调用失败。消息里只放平台返回的错误码与说明，不放 token、secret、完整手机号。
 */
public class ImPushException extends Exception {

    public ImPushException(String message) {
        super(message);
    }

    public ImPushException(String message, Throwable cause) {
        super(message, cause);
    }

    /** 平台判定访问 token 无效或过期，调用方应刷新 token 后重试一次。 */
    public static class TokenInvalid extends ImPushException {
        public TokenInvalid(String message) {
            super(message);
        }
    }
}
