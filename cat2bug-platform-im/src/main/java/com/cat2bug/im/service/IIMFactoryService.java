package com.cat2bug.im.service;

import com.cat2bug.im.domain.IMConfig;
import com.cat2bug.im.domain.IMMessage;

import java.util.List;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-07-07 01:49
 * @Version: 1.0.0
 */
public interface IIMFactoryService {
    public <T> List<IMMessage> createMessage(Long projectId, String group, Long senderId, List<Long> recipientIds, String title, T text, String src, IMessageTemplate<T> messageTemplate, IMConfig config);
}
