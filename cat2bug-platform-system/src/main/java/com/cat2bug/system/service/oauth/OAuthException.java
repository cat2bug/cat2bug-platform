package com.cat2bug.system.service.oauth;

/**
 * OAuth 协议错误（RFC 6749 第 5.2 节 / RFC 7591 第 3.2.2 节的 {@code error} 取值）。
 * {@code redirectable=false} 表示 client 或 redirect_uri 不可信，不能把错误跳回给调用方。
 */
public class OAuthException extends RuntimeException {

    private final String error;
    private final int httpStatus;
    private final boolean redirectable;

    public OAuthException(String error, String description) {
        this(error, description, 400, true);
    }

    public OAuthException(String error, String description, int httpStatus, boolean redirectable) {
        super(description);
        this.error = error;
        this.httpStatus = httpStatus;
        this.redirectable = redirectable;
    }

    /** 授权端点上已确认可信的回调地址，错误可带回给客户端 */
    private String redirectUri;

    public static OAuthException notRedirectable(String error, String description) {
        return new OAuthException(error, description, 400, false);
    }

    public static OAuthException redirectTo(String redirectUri, String error, String description) {
        OAuthException e = new OAuthException(error, description);
        e.redirectUri = redirectUri;
        return e;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public String getError() {
        return error;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public boolean isRedirectable() {
        return redirectable;
    }
}
