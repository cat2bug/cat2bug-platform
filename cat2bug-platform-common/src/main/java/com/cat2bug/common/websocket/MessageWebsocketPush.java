package com.cat2bug.common.websocket;

import com.cat2bug.common.api.WebSocketResult;

/**
 * WebSocket 推送门面，供 IM / 通知 / 在线状态等业务调用（与 Spring {@code MessageWebsocket} 对齐）。
 */
public interface MessageWebsocketPush {

    void sendToAll(WebSocketResult result);

    void sendToMember(Long memberId, WebSocketResult result);

    boolean isMemberConnected(Long memberId);
}
