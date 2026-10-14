package com.cat2bug.common.api;

/**
 * 与 Spring {@code WebSocketResult} JSON 形态兼容（code / msg / data / action）。
 */
public class WebSocketResult extends AjaxResult {

    public static final String ACTION_TAG = "action";

    public WebSocketResult(int code, String msg, Object data, String action) {
        super(code, msg, data);
        put(ACTION_TAG, action);
    }

    public static WebSocketResult success(String action, Object data) {
        return new WebSocketResult(HTTP_OK, "操作成功", data, action);
    }
}
