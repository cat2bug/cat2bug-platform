package com.cat2bug.common.websocket;

import com.alibaba.fastjson.JSON;
import com.cat2bug.common.config.WebSocketSpringConfigurator;
import com.cat2bug.common.core.domain.WebSocketResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    @Autowired
    private List<IWebSocketService> services;

    @OnOpen
    public void onOpen(Session session,@PathParam(value="memberId")Long memberId) {
        this.sessions.add(session);
        this.memberId = memberId;
        this.services.forEach(s->{
            s.onOpen(MessageWebsocket.this, session,memberId);
        });
    }

    @OnClose
    public void onClose(Session session) {
        this.sessions.remove(session);
        log.info("【websocket消息】连接断开, 总数:{}", sessions.size());

        this.services.forEach(s->{
            s.onClose(session);
        });
    }

    @OnMessage
    public void onMessage(String message) {
        log.info("【websocket消息】收到客户端发来的消息:{}", message);

        this.services.forEach(s->{
            s.onMessage(message);
        });
    }

//    @Async
    public synchronized void sendMessage(WebSocketResult result) {
        for (Session s : sessions) {
            try {
                s.getBasicRemote().sendText(JSON.toJSONString(result));
            }catch (Exception e) {
                log.error(e);
            }
        }
    }
}
