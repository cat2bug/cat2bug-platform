package com.cat2bug.im.service;

import com.cat2bug.im.domain.IMMessage;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-07-07 01:49
 * @Version: 1.0.0
 */
public interface IIMService<T extends IMMessage> extends IIMListener<T> {
    /**
     * 获取消息工厂名称
     * @return
     */
    public String getMessageFactoryName();

    /**
     * 发送消息
     * @param message   消息
     * @return          发送是否成功
     */
    public void sendNoticeMessage(T message) throws Exception;
}
