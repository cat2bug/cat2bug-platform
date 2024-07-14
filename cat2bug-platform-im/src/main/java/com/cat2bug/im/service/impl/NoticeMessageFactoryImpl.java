package com.cat2bug.im.service.impl;

import com.cat2bug.im.domain.*;
import com.cat2bug.im.mapper.MemberMapper;
import com.cat2bug.im.service.IIMFactoryService;
import com.cat2bug.im.service.IMessageTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-07-07 21:23
 * @Version: 1.0.0
 */
@Service(NoticeMessageServiceImpl.MESSAGE_FACTORY_NAME)
public class NoticeMessageFactoryImpl implements IIMFactoryService {
    @Autowired
    private MemberMapper memberMapper;

    @Value("${spring.mail.from}")
    private String defaultMail;

    @Override
    public <T> List<IMMessage> createMessage(Long projectId, String group, Long senderId, List<Long> recipientIds, String title, T content, IMessageTemplate<T> messageTemplate) {
        if(recipientIds==null) return new ArrayList<>();
        String text = messageTemplate.toMarkdown(content);
        return recipientIds.stream().map(r->{
            NoticeMessage msg = new NoticeMessage();
            msg.setProjectId(projectId);
            msg.setGroup(group);
            msg.setTitle(title);
            msg.setType(IMMessageType.MARKDOWN);
            msg.setContent(text);
            msg.setNoticeType((char)1);
            msg.setStatus((char)0);
            msg.setReceiveMemberId(r);
            return msg;
        }).collect(Collectors.toList());
    }
}
