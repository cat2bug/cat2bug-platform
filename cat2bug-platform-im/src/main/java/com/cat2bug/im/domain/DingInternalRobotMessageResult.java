package com.cat2bug.im.domain;

import lombok.Data;

import java.util.Map;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-07-28 02:06
 * @Version: 1.0.0
 * 钉钉内部机器人消息返回信息
 */
@Data
public class DingInternalRobotMessageResult {
    /**
     * 发送的DING消息Id
     */
    private String openDingId;
    /**
     * 失败的接收者列表，格式为 {"错误原因"：[user01, user02]}
     */
    private Map<String, Object> failedList;
}
