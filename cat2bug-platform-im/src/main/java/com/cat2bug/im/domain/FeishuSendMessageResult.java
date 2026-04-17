package com.cat2bug.im.domain;

import lombok.Data;

/**
 * 飞书发送消息返回结果
 */
@Data
public class FeishuSendMessageResult {
    private int code;
    private String msg;
}
