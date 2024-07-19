package com.cat2bug.im.service.impl;

import com.cat2bug.common.core.domain.WebSocketResult;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.common.websocket.IWebSocketService;
import com.cat2bug.common.websocket.MessageWebsocket;
import com.cat2bug.im.domain.IMSystemPlatformConfig;
import com.cat2bug.im.domain.NoticeMessage;
import com.cat2bug.im.mapper.NoticeMapper;
import com.cat2bug.im.service.IIMService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.websocket.Session;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-07-08 00:15
 * @Version: 1.0.0
 * 通知消息服务
 */
@Service
public class PanelNoticeMessageServiceImpl implements IIMService<NoticeMessage, IMSystemPlatformConfig>, IWebSocketService {
    private final static Logger log = LogManager.getLogger(PanelNoticeMessageServiceImpl.class);

    private final static String NOTICE_ACTION = "panel_notice";

    public final static String MESSAGE_FACTORY_NAME = "ImPanelNoticeMessage";
    @Autowired
    private NoticeMapper noticeMapper;

    private static Map<Long, MessageWebsocket> memberWebSocketMap = new ConcurrentHashMap<>();
    /**
     * 接口内容类型
     */
    @Override
    public void sendNoticeMessage(NoticeMessage message, IMSystemPlatformConfig config) throws Exception {
        if(StringUtils.isNotBlank(message.getTitle()) && message.getTitle().length()>255) {
            message.setTitle(message.getTitle().substring(0,255));
        }
        noticeMapper.insertNotice(message);
        if(this.memberWebSocketMap.containsKey(message.getReceiveMemberId())) {
            WebSocketResult ws = WebSocketResult.success(NOTICE_ACTION, message);
            this.memberWebSocketMap.get(message.getReceiveMemberId()).sendMessage(ws);
        }
    }

    @Override
    public String getMessageFactoryName() {
        return MESSAGE_FACTORY_NAME;
    }

    @Override
    public void receiveMessage(NoticeMessage message) {

    }

    @Override
    public void onOpen(MessageWebsocket messageWebsocket, Long memberId, Session session) {
        this.memberWebSocketMap.put(memberId, messageWebsocket);
    }

    @Override
    public void onClose(MessageWebsocket messageWebsocket, Long memberId, Session session) {
        this.memberWebSocketMap.remove(memberId);
    }

    @Override
    public void onMessage(MessageWebsocket messageWebsocket, Long memberId, String message) {

    }
}
