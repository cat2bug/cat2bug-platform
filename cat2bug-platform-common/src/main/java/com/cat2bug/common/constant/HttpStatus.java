package com.cat2bug.common.constant;

/** 对齐 Spring {@code com.cat2bug.common.constant.HttpStatus}。 */
public final class HttpStatus {

    public static final int SUCCESS = 200;
    public static final int BAD_REQUEST = 400;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;
    public static final int CONFLICT = 409;
    public static final int ERROR = 500;

    private HttpStatus() {
    }
}
