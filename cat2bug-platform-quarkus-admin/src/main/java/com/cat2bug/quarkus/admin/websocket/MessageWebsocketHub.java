package com.cat2bug.quarkus.admin.websocket;

import com.cat2bug.quarkus.common.api.WebSocketResult;
import com.cat2bug.quarkus.common.websocket.MessageWebsocketPush;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.Session;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 会话注册表与推送实现，移植自 Spring {@code MessageWebsocket} + {@code SysUserOnlineServiceImpl} 在线计数。
 */
@ApplicationScoped
public class MessageWebsocketHub implements MessageWebsocketPush {

    private static final Logger LOG = Logger.getLogger(MessageWebsocketHub.class);

    public static final String MEMBER_ONLINE = "memberOnline";
    static final String MEMBER_OFFLINE = "memberOffline";

    private final Map<Session, Long> sessionMap = new ConcurrentHashMap<>();
    private final Map<Long, AtomicInteger> onlineRefCount = new ConcurrentHashMap<>();

    @Inject
    ObjectMapper objectMapper;

    public void onOpen(Session session, Long memberId) {
        sessionMap.put(session, memberId);
        memberOnline(memberId, true);
        LOG.debug("WebSocket 已连接 memberId=" + memberId + " session=" + session.getId() + " 总数=" + sessionMap.size());
    }

    public void onClose(Session session) {
        Long memberId = sessionMap.remove(session);
        if (memberId != null) {
            memberOffline(memberId);
            LOG.debug("WebSocket 已断开 memberId=" + memberId + " session=" + session.getId() + " 总数=" + sessionMap.size());
        }
    }

    public void onMessage(Long memberId, String message) {
        LOG.debugf("WebSocket 客户端消息 memberId=%s: %s", memberId, message);
    }

    @Override
    public synchronized void sendToAll(WebSocketResult result) {
        String json = toJson(result);
        for (Session session : sessionMap.keySet()) {
            sendText(session, json);
        }
    }

    @Override
    public synchronized void sendToMember(Long memberId, WebSocketResult result) {
        if (memberId == null) {
            return;
        }
        String json = toJson(result);
        for (Map.Entry<Session, Long> entry : sessionMap.entrySet()) {
            if (memberId.equals(entry.getValue())) {
                sendText(entry.getKey(), json);
            }
        }
    }

    @Override
    public boolean isMemberConnected(Long memberId) {
        return memberId != null && onlineRefCount.containsKey(memberId);
    }

    int sessionCount() {
        return sessionMap.size();
    }

    private void memberOnline(Long memberId, boolean deferBroadcast) {
        onlineRefCount.computeIfAbsent(memberId, ignored -> new AtomicInteger()).incrementAndGet();
        WebSocketResult result = WebSocketResult.success(MEMBER_ONLINE, memberId);
        if (deferBroadcast) {
            // Undertow 在 @OnOpen 回调内同步 sendText 会静默失败，需延后广播
            Thread thread = new Thread(() -> sendToAll(result), "ws-member-online-" + memberId);
            thread.setDaemon(true);
            thread.start();
        } else {
            sendToAll(result);
        }
    }

    private void memberOffline(Long memberId) {
        AtomicInteger counter = onlineRefCount.get(memberId);
        if (counter == null) {
            return;
        }
        if (counter.decrementAndGet() <= 0) {
            onlineRefCount.remove(memberId);
            sendToAll(WebSocketResult.success(MEMBER_OFFLINE, memberId));
        }
    }

    private String toJson(WebSocketResult result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("WebSocket 消息序列化失败", e);
        }
    }

    private void sendText(Session session, String json) {
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            session.getBasicRemote().sendText(json);
        } catch (IOException e) {
            LOG.debugf(e, "WebSocket 发送失败 session=%s", session.getId());
        }
    }
}
