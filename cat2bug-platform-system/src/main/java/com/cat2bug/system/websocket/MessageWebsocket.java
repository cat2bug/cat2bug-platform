package com.cat2bug.system.websocket;

import com.alibaba.fastjson.JSON;
import com.cat2bug.common.config.WebSocketSpringConfigurator;
import com.cat2bug.common.core.domain.WebSocketResult;
import com.cat2bug.system.service.IMemberFocusService;
import com.cat2bug.system.service.ISysUserOnlineService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-03-21 14:49
 * @Version: 1.0.0
 */
@Component
@ServerEndpoint(value="/websocket/{memberId}/message", configurator = WebSocketSpringConfigurator.class)
public class MessageWebsocket {

    private static Set<Session> sessions = Collections.synchronizedSet(new HashSet<>());


    private final static Logger log = LogManager.getLogger(MessageWebsocket.class);

    private Long memberId;

    @Lazy
    @Autowired
    private IMemberFocusService memberFocusService;

    @Lazy
    @Autowired
    private ISysUserOnlineService sysUserOnlineService;

    @OnOpen
    public void onOpen(Session session,@PathParam(value="memberId")Long memberId) {
        this.sessions.add(session);
        this.memberId = memberId;

        sysUserOnlineService.memberOnline(memberId);
        log.info("【websocket消息】有新的连接, 总数:{}", sessions.size());
    }

    @OnClose
    public void onClose(Session session) {
        this.sessions.remove(session);
        // 移除成员焦点
        memberFocusService.removeFocus(this.memberId);
        // 设置成员离线
        sysUserOnlineService.memberOffline(memberId);
        log.info("【websocket消息】连接断开, 总数:{}", sessions.size());
    }

    @OnMessage
    public void onMessage(String message) {
        log.info("【websocket消息】收到客户端发来的消息:{}", message);
    }

    @Async
    public void sendMessage(WebSocketResult result) {
        for (Session s : sessions) {
            s.getAsyncRemote().sendText(JSON.toJSONString(result));
        }
    }
}
