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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-03-21 14:49
 * @Version: 1.0.0
 */
@Component
@ServerEndpoint(value="/websocket/{memberId}/message", configurator = WebSocketSpringConfigurator.class)
public class MessageWebsocket {

    private static Map<Session,Long> sessionMap = new ConcurrentHashMap<>();

    private final static Logger log = LogManager.getLogger(MessageWebsocket.class);

    private Long memberId;

    @Autowired
    private List<IWebSocketService> services;

    @OnOpen
    public void onOpen(Session session,@PathParam(value="memberId")Long memberId) {
        this.sessionMap.put(session,memberId);
        this.memberId = memberId;
        this.services.forEach(s->{
            s.onOpen(this, memberId, session);
        });
    }

    @OnClose
    public void onClose(Session session) {
        if(this.sessionMap.containsKey(session)) {
            Long member = this.sessionMap.get(session);
            this.sessionMap.remove(session);
            log.info("【websocket消息】连接断开, 总数:{}", sessionMap.size());

            this.services.forEach(s -> {
                s.onClose(this, member, session);
            });
        }
    }

    @OnMessage
    public void onMessage(String message) {
        log.info("【websocket消息】收到客户端发来的消息:{}", message);

        this.services.forEach(s->{
            s.onMessage(this, this.memberId, message);
        });
    }

    /**
     * 发送消息给所有人
     * @param result    发送的消息
     */
//    @Async
    public synchronized void sendMessage(WebSocketResult result) {
        for (Session s : sessionMap.keySet()) {
            try {
                s.getBasicRemote().sendText(JSON.toJSONString(result));
            }catch (Exception e) {
                log.error(e);
            }
        }
    }

    /**
     * 发送给指定成员消息
     * @param memberId  成员ID
     * @param result    发送的信息
     */
    public synchronized void sendMessage(Long memberId, WebSocketResult result) {
        for(Map.Entry<Session, Long> item : this.sessionMap.entrySet()){
            if(item.getValue().equals(memberId)==false) continue;
            try {
                Session s = item.getKey();
                s.getBasicRemote().sendText(JSON.toJSONString(result));
            }catch (Exception e) {
                log.error(e);
            }
        }
    }
}
