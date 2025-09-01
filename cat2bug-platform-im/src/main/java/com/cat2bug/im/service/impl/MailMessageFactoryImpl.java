package com.cat2bug.im.service.impl;

import com.cat2bug.common.utils.StringUtils;
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
@Service(MailMessageServiceImpl.MESSAGE_FACTORY_NAME)
public class MailMessageFactoryImpl implements IIMFactoryService {
    @Autowired
    private MemberMapper memberMapper;

    @Value("${spring.mail.from}")
    private String defaultMail;

    @Override
    public <T> List<IMMessage> createMessage(Long projectId, String group, Long senderId, List<Long> recipientIds, String title, T content, String src, IMessageTemplate<T> messageTemplate, IMConfig config) {
        List<Member> recipientList = this.memberMapper.selectMemberList(recipientIds);
        if(recipientList==null) return new ArrayList<>();

        String text = messageTemplate.toHtml(content,config.getModules());
        if(StringUtils.isBlank(text)) return new ArrayList<>();
        return recipientList.stream().map(r->{
            MailMessage msg = new MailMessage();
            msg.setSrc(src);
            msg.setGroup(group);
            msg.setFrom(this.defaultMail);
            msg.setTo(r.getMail());
            msg.setTitle(title);
            msg.setType(IMMessageType.HTML);
            msg.setContent(text);
            return msg;
        }).collect(Collectors.toList());
    }
}
