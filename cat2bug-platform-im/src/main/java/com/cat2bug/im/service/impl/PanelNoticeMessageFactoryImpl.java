package com.cat2bug.im.service.impl;

import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.im.domain.*;
import com.cat2bug.im.service.IIMFactoryService;
import com.cat2bug.im.service.IMessageTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-07-07 21:23
 * @Version: 1.0.0
 */
@Service(PanelNoticeMessageServiceImpl.MESSAGE_FACTORY_NAME)
public class PanelNoticeMessageFactoryImpl implements IIMFactoryService {
    @Override
    public <T> List<IMMessage> createMessage(Long projectId, String group, Long senderId, List<Long> recipientIds, String title, T content, String src, IMessageTemplate<T> messageTemplate,IMConfig config) {
        IMSystemPlatformConfig systemConfig = config.getPlatforms().getSystem();
        // 如果接收人集合为空或者通知面板为false，则不发送
        if(recipientIds==null || systemConfig.isPanel()==false) return new ArrayList<>();
        // 通过模版获取内容字符串
        String text = messageTemplate.toHtml(content, config.getModules());
        // 如果字符串为空，则不发送
        if(StringUtils.isBlank(text)) return new ArrayList<>();
        // 返回需要发送的数据集合
        return recipientIds.stream().map(r->{
            NoticeMessage msg = new NoticeMessage();
            msg.setSrc(src);
            msg.setProjectId(projectId);
            msg.setGroup(group);
            msg.setTitle(title);
            msg.setType(IMMessageType.MARKDOWN);
            msg.setContent(text);
            msg.setNoticeType((char)1);
            msg.setStatus((char)0);
            msg.setReceiveMemberId(r);
            msg.setBackgroundMusic(systemConfig.isBackgroundMusic());
            msg.setBackgroundMusicUrl(systemConfig.getBackgroundMusicUrl());
            msg.setPanel(systemConfig.isPanel());
            return msg;
        }).collect(Collectors.toList());
    }
}
