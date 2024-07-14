package com.cat2bug.system.websocket;

import com.cat2bug.common.websocket.IWebSocketService;
import com.cat2bug.common.websocket.MessageWebsocket;
import com.cat2bug.system.service.IMemberFocusService;
import com.cat2bug.system.service.ISysUserOnlineService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.websocket.Session;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-03-21 14:49
 * @Version: 1.0.0
 */
@Component
public class MemberFocusWebsocket implements IWebSocketService {
    private final static Logger log = LogManager.getLogger(MemberFocusWebsocket.class);

    private Long memberId;

    @Lazy
    @Autowired
    private IMemberFocusService memberFocusService;

    @Lazy
    @Autowired
    private ISysUserOnlineService sysUserOnlineService;

    @Override
    public void onOpen(com.cat2bug.common.websocket.MessageWebsocket messageWebsocket, Long memberId, Session session) {
        this.memberId = memberId;
        sysUserOnlineService.memberOnline(memberId);
    }

    @Override
    public void onClose(MessageWebsocket messageWebsocket, Long memberId, Session session) {
        // 移除成员焦点
        memberFocusService.removeFocus(this.memberId);
        // 设置成员离线
        sysUserOnlineService.memberOffline(memberId);
    }

    @Override
    public void onMessage(MessageWebsocket messageWebsocket, Long memberId, String message) {

    }
}
