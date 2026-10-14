package com.cat2bug.common.api;

import java.util.HashMap;

/**
 * 与 Spring 版 AjaxResult 字段形态兼容（code / msg / data）。
 */
public class AjaxResult extends HashMap<String, Object> {

    public static final String CODE_TAG = "code";
    public static final String MSG_TAG = "msg";
    public static final String DATA_TAG = "data";
    public static final String TOKEN_TAG = "token";

    public static final int HTTP_OK = 200;
    public static final int HTTP_UNAUTHORIZED = 401;
    public static final int HTTP_FORBIDDEN = 403;
    public static final int HTTP_WARN = 601;
    public static final int HTTP_ERROR = 500;

    public AjaxResult() {
    }

    public AjaxResult(int code, String msg, Object data) {
        put(CODE_TAG, code);
        put(MSG_TAG, msg);
        if (data != null) {
            put(DATA_TAG, data);
        }
    }

    public static AjaxResult success() {
        return success("操作成功", null);
    }

    public static AjaxResult success(Object data) {
        return success("操作成功", data);
    }

    public static AjaxResult success(String msg, Object data) {
        return new AjaxResult(HTTP_OK, msg, data);
    }

    public static AjaxResult error(String msg) {
        return new AjaxResult(HTTP_ERROR, msg, null);
    }

    public static AjaxResult error(int code, String msg) {
        return new AjaxResult(code, msg, null);
    }

    public static AjaxResult warn(String msg) {
        return new AjaxResult(HTTP_WARN, msg, null);
    }
}
