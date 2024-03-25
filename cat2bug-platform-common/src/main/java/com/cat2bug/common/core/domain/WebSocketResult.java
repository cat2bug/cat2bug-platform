package com.cat2bug.common.core.domain;

import com.cat2bug.common.constant.HttpStatus;
import lombok.Data;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-03-22 12:15
 * @Version: 1.0.0
 */
@Data
public class WebSocketResult extends AjaxResult {
    /**
     * 消息类型
     */
    private String action;

    public WebSocketResult(int code, String msg, Object data, String action) {
        super(code,msg,data);
        super.put("action",action);
    }

    public static WebSocketResult success(String action, Object data)
    {
        return new WebSocketResult(HttpStatus.SUCCESS, "操作成功", data, action);
    }
}
