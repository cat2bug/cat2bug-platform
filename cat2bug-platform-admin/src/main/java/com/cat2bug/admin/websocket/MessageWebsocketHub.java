package com.cat2bug.admin.websocket;

import com.cat2bug.common.api.WebSocketResult;
import com.cat2bug.common.websocket.MessageWebsocketPush;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.Session;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 会话注册表与推送实现，移植自 Spring {@code MessageWebsocket} + {@code SysUserOnlineServiceImpl} 在线计数。
 * <p>
 * 发送时不持有 hub 监视器，也不使用阻塞的 BasicRemote.sendText：否则 onClose 里再广播
 * 会与发送线程死锁，拖垮 worker 导致 /setup/status、验证码等请求挂起。
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
        memberOnline(memberId);
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
    public void sendToAll(WebSocketResult result) {
        String json = toJson(result);
        for (Session session : snapshotSessions()) {
            sendText(session, json);
        }
    }

    @Override
    public void sendToMember(Long memberId, WebSocketResult result) {
        if (memberId == null) {
            return;
        }
        String json = toJson(result);
        for (Map.Entry<Session, Long> entry : snapshotEntries()) {
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

    private void memberOnline(Long memberId) {
        onlineRefCount.computeIfAbsent(memberId, ignored -> new AtomicInteger()).incrementAndGet();
        // Undertow 在 @OnOpen 回调内同步 send 会静默失败；延后广播，且不占用事件线程
        deferBroadcast(WebSocketResult.success(MEMBER_ONLINE, memberId), "ws-member-online-" + memberId);
    }

    private void memberOffline(Long memberId) {
        AtomicInteger counter = onlineRefCount.get(memberId);
        if (counter == null) {
            return;
        }
        if (counter.decrementAndGet() <= 0) {
            onlineRefCount.remove(memberId);
            // onClose 路径可能仍持有 Undertow/Netty 锁，延后广播避免与 send 死锁
            deferBroadcast(WebSocketResult.success(MEMBER_OFFLINE, memberId), "ws-member-offline-" + memberId);
        }
    }

    private void deferBroadcast(WebSocketResult result, String threadName) {
        Thread thread = new Thread(() -> sendToAll(result), threadName);
        thread.setDaemon(true);
        thread.start();
    }

    private List<Session> snapshotSessions() {
        return new ArrayList<>(sessionMap.keySet());
    }

    private List<Map.Entry<Session, Long>> snapshotEntries() {
        return new ArrayList<>(sessionMap.entrySet());
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
            // 异步发送，避免 BasicRemote 阻塞等待与 onClose 形成死锁
            session.getAsyncRemote().sendText(json);
        } catch (Exception e) {
            LOG.debugf(e, "WebSocket 发送失败 session=%s", session.getId());
        }
    }
}
