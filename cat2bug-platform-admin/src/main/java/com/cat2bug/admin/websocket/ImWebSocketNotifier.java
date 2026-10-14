package com.cat2bug.admin.websocket;

import com.cat2bug.common.api.WebSocketResult;
import com.cat2bug.common.websocket.MessageWebsocketPush;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * IM 通知 WebSocket 推送辅助，对齐 Spring {@code NoticeMessageServiceImpl} / {@code PanelNoticeMessageServiceImpl}。
 */
@ApplicationScoped
public class ImWebSocketNotifier {

    public static final String NOTICE_ACTION = "notice";
    public static final String PANEL_NOTICE_ACTION = "panel_notice";

    @Inject
    MessageWebsocketPush messageWebsocketPush;

    public void pushNotice(Long receiveMemberId, Object noticePayload) {
        if (receiveMemberId == null || !messageWebsocketPush.isMemberConnected(receiveMemberId)) {
            return;
        }
        messageWebsocketPush.sendToMember(receiveMemberId, WebSocketResult.success(NOTICE_ACTION, noticePayload));
    }

    public void pushPanelNotice(Long receiveMemberId, Object noticePayload) {
        if (receiveMemberId == null || !messageWebsocketPush.isMemberConnected(receiveMemberId)) {
            return;
        }
        messageWebsocketPush.sendToMember(receiveMemberId, WebSocketResult.success(PANEL_NOTICE_ACTION, noticePayload));
    }
}
