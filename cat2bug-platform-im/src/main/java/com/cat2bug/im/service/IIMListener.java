package com.cat2bug.im.service;

import com.cat2bug.im.domain.IMMessage;

/**
 * 消息监听
 */
public interface IIMListener<T extends IMMessage> {
    public void receiveMessage(T message);
}
