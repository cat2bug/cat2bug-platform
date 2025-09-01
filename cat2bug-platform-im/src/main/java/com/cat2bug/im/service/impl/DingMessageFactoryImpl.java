package com.cat2bug.im.service.impl;

import com.alibaba.fastjson.JSON;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.im.domain.*;
import com.cat2bug.im.mapper.MemberMapper;
import com.cat2bug.im.service.IIMFactoryService;
import com.cat2bug.im.service.IIMProjectConfigService;
import com.cat2bug.im.service.IMessageTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
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

    @Autowired
    private IIMProjectConfigService imProjectConfigService;

    @Override
    public <T> List<IMMessage> createMessage(Long projectId, String group, Long senderId, List<Long> recipientIds, String title, T content, String src, IMessageTemplate<T> messageTemplate,IMConfig config) {
        List<Member> recipientList = this.memberMapper.selectMemberList(recipientIds);
        if(recipientList==null) return new ArrayList<>();
        String text = messageTemplate.toText(content, config.getModules());
        if(StringUtils.isBlank(text)) return new ArrayList<>();
        String finalText = String.format("【%s】%s", config.getPlatforms().getDing().getKey(), text);

        // 获取配置
        IMProjectConfig projectConfig = imProjectConfigService.selectImProjectConfigByProjectIdAndSystemCode(projectId, DingProjectConfig.SYSTEM_CODE);
        DingProjectConfig dingProjectConfig;
        if(projectConfig!=null) {
            dingProjectConfig = JSON.parseObject(projectConfig.getConfigParams(), DingProjectConfig.class);
        } else {
            dingProjectConfig = null;
        }

        String markdownText = messageTemplate.toMarkdown(content, config.getModules());
        return recipientList.stream().map(r->{
            DingMessage msg = new DingMessage(finalText);
            msg.setProjectId(projectId);
            msg.setSrc(src);
            msg.setMsgtype("text");

            // 群发消息
            if(dingProjectConfig!=null && StringUtils.isNotBlank(config.getPlatforms().getDing().getUserId())) {
                msg.setAppKey(dingProjectConfig.getAppKey());
                msg.setAppSecret(dingProjectConfig.getAppSecret());
                msg.setRobotCode(dingProjectConfig.getRobotCode());
                msg.setUserIds(Arrays.asList(config.getPlatforms().getDing().getUserId()));
                DingSampleActionCardParams params = new DingSampleActionCardParams();
                params.setTitle(title);
                params.setText(markdownText);
                params.setSingleTitle("点击查看详情");
                // 链接加全屏界面
                params.setSingleURL(src + "&screen=full");
                msg.setMsgParam(JSON.toJSONString(params));
            }
            return msg;
        }).collect(Collectors.toList());
    }
}
