package com.cat2bug.im.service.impl;

import com.alibaba.fastjson.JSON;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.im.domain.*;
import com.cat2bug.im.service.IIMFactoryService;
import com.cat2bug.im.service.IIMProjectConfigService;
import com.cat2bug.im.service.IMessageTemplate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-07-18 00:31
 * @Version: 1.0.0
 * 飞书消息工厂
 */
@Service(FeishuMessageServiceImpl.MESSAGE_FACTORY_NAME)
public class FeishuMessageFactoryImpl implements IIMFactoryService {
    private final static Logger log = LogManager.getLogger(FeishuMessageFactoryImpl.class);

    @Autowired
    private IIMProjectConfigService imProjectConfigService;

    @Override
    public <T> List<IMMessage> createMessage(Long projectId, String group, Long senderId, List<Long> recipientIds,
                                              String title, T content, String src,
                                              IMessageTemplate<T> messageTemplate, IMConfig config) {
        if (recipientIds == null) return new ArrayList<>();

        String text = messageTemplate.toText(content, config.getModules());
        if (StringUtils.isBlank(text)) return new ArrayList<>();

        // 仅使用项目级飞书群机器人配置
        IMProjectConfig projectConfig = imProjectConfigService.selectImProjectConfigByProjectIdAndSystemCode(
                projectId, FeishuProjectConfig.SYSTEM_CODE);
        if (projectConfig == null || StringUtils.isBlank(projectConfig.getConfigParams())) {
            log.warn("飞书群机器人未配置（项目级），无法发送消息 projectId={}", projectId);
            return new ArrayList<>();
        }
        FeishuProjectConfig feishuProjectConfig = JSON.parseObject(projectConfig.getConfigParams(), FeishuProjectConfig.class);
        String hook = feishuProjectConfig.getHook();
        String secret = feishuProjectConfig.getSecret();

        if (StringUtils.isBlank(hook)) {
            log.warn("飞书群机器人 Webhook 地址为空，无法发送消息 projectId={}", projectId);
            return new ArrayList<>();
        }

        final String finalHook = hook;
        final String finalSecret = secret;

        return recipientIds.stream().map(r -> {
            FeishuMessage msg = new FeishuMessage(text);
            msg.setProjectId(projectId);
            msg.setSrc(src);
            msg.setGroup(group);
            msg.setTitle(title);
            msg.setReceiveMemberId(r);
            msg.setHook(finalHook);
            msg.setSecret(finalSecret);
            return (IMMessage) msg;
        }).collect(Collectors.toList());
    }
}
