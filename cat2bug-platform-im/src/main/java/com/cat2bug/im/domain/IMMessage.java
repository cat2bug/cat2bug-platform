package com.cat2bug.im.domain;

import lombok.Data;

import java.util.Date;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-07-07 01:49
 * @Version: 1.0.0
 */
@Data
public class IMMessage<T> {
    /**
     * 消息流水号
     */
    private String sn;
    /**
     * 内容
     */
    private T content;
    /**
     * 标题
     */
    private String title;
    /**
     * 通知源
     */
    private String src;
    /**
     * 消息类型
     */
    private IMMessageType type;
    /**
     * 接收成员ID
     */
    private Long receiveMemberId;
    /**
     * 时间戳
     */
    private Date timestamp;
    /**
     * 分组名称
     */
    private String group;
    /**
     * 项目ID
     */
    private Long projectId;
}
