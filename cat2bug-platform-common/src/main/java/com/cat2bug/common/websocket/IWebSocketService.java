package com.cat2bug.common.websocket;

import javax.websocket.Session;

/**
 * websocket服务
 */
public interface IWebSocketService {
    /**
     * 打开客户端
     * @param messageWebsocket
     * @param session
     * @param memberId
     */
    public void onOpen(MessageWebsocket messageWebsocket, Session session, Long memberId);

    /**
     * 关闭客户端
     * @param session
     */
    public void onClose(Session session);

    /**
     * 接收到消息
     * @param message   消息
     */
    public void onMessage(String message);
}
