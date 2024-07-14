package com.cat2bug.im.service.impl;

import com.cat2bug.im.domain.DingMessage;
import com.cat2bug.im.domain.IMMessage;
import com.cat2bug.im.domain.Member;
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
@Service(DingMessageServiceImpl.MESSAGE_FACTORY_NAME)
public class DingMessageFactoryImpl implements IIMFactoryService {
    @Autowired
    private MemberMapper memberMapper;

    @Value("${spring.mail.from}")
    private String defaultMail;

    @Override
    public <T> List<IMMessage> createMessage(Long projectId, String group, Long senderId, List<Long> recipientIds, String title, T content, IMessageTemplate<T> messageTemplate) {
        List<Member> recipientList = this.memberMapper.selectMemberList(recipientIds);
        if(recipientList==null) return new ArrayList<>();
        String text = messageTemplate.toText(content);
        return recipientList.stream().map(r->{
            DingMessage msg = new DingMessage(text);
            msg.setWebHook("https://oapi.dingtalk.com/robot/send?access_token=4a70182b952466a5e2f11caed3ebc7a40eeb656bd670bd5320ad30268e15c697");
            msg.setMsgtype("text");
            return msg;
        }).collect(Collectors.toList());
    }
}
