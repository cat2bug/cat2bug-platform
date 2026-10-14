package com.cat2bug.admin.websocket;

import com.cat2bug.system.service.MemberFocusService;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.jboss.logging.Logger;

/**
 * 与 Spring {@code MessageWebsocket} 路径对齐：{@code /websocket/{memberId}/message}。
 */
@ServerEndpoint(value = "/websocket/{memberId}/message")
public class MessageWebsocketEndpoint {

    private static final Logger LOG = Logger.getLogger(MessageWebsocketEndpoint.class);

    private MessageWebsocketHub hub() {
        return CDI.current().select(MessageWebsocketHub.class).get();
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("memberId") Long memberId) {
        hub().onOpen(session, memberId);
    }

    @OnClose
    public void onClose(Session session, @PathParam("memberId") Long memberId) {
        hub().onClose(session);
        CDI.current().select(MemberFocusService.class).get().removeFocus(memberId);
    }

    @OnMessage
    public void onMessage(String message, @PathParam("memberId") Long memberId) {
        hub().onMessage(memberId, message);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        LOG.debugf(error, "WebSocket 异常 session=%s", session != null ? session.getId() : "null");
    }
}
