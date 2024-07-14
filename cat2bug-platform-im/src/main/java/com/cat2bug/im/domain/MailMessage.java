package com.cat2bug.im.domain;

import lombok.Data;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-07-07 12:43
 * @Version: 1.0.0
 * 邮件消息
 */
@Data
public class MailMessage extends IMMessage<String> {
    /**
     * 发送邮箱
     */
    private String from;
    /**
     * 接收邮箱
     */
    private String to;

}
