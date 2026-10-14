package com.cat2bug.admin;

import com.cat2bug.admin.websocket.ImWebSocketNotifier;
import com.cat2bug.admin.websocket.MessageWebsocketHub;
import com.cat2bug.common.api.WebSocketResult;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.OnMessage;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class QuarkusWebsocketTest {

    private static final long MEMBER_ID = 1L;

    @TestHTTPResource("/websocket/" + MEMBER_ID + "/message")
    URI websocketUri;

    @Inject
    MessageWebsocketHub hub;

    @Inject
    ImWebSocketNotifier imWebSocketNotifier;

    @Test
    void connectAndReceiveMemberTargetedNotice() throws Exception {
        TestWebSocketClient client = new TestWebSocketClient();
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        Session session = container.connectToServer(client, websocketUri);
        try {
            assertTrue(client.awaitOpen(5, TimeUnit.SECONDS));
            assertTrue(client.awaitMessage(5, TimeUnit.SECONDS));
            assertEquals(MessageWebsocketHub.MEMBER_ONLINE, client.lastAction());

            client.resetMessageLatch();
            imWebSocketNotifier.pushNotice(MEMBER_ID, MapLikeNotice.of("title", "Quarkus WS 测试"));

            assertTrue(client.awaitMessage(5, TimeUnit.SECONDS));
            assertEquals(200, client.lastCode());
            assertEquals(ImWebSocketNotifier.NOTICE_ACTION, client.lastAction());
            assertEquals("Quarkus WS 测试", client.lastDataTitle());
        } finally {
            session.close();
        }
    }

    @Test
    void hubBroadcastsMemberOnlineOnConnect() throws Exception {
        TestWebSocketClient client = new TestWebSocketClient();
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        Session session = container.connectToServer(client, websocketUri);
        try {
            assertTrue(client.awaitMessage(5, TimeUnit.SECONDS));
            assertEquals(200, client.lastCode());
            assertEquals(MessageWebsocketHub.MEMBER_ONLINE, client.lastAction());
            assertEquals(MEMBER_ID, client.lastDataMemberId());
        } finally {
            session.close();
        }
    }

    @Test
    void hubSendToMemberWithoutConnectionDoesNotThrow() {
        hub.sendToMember(99_999L, WebSocketResult.success("notice", MapLikeNotice.of("title", "noop")));
    }

    @ClientEndpoint
    static class TestWebSocketClient {

        private final CountDownLatch openLatch = new CountDownLatch(1);
        private CountDownLatch messageLatch = new CountDownLatch(1);
        private String lastMessage;

        @jakarta.websocket.OnOpen
        public void onOpen(Session session) {
            openLatch.countDown();
        }

        @OnMessage
        public void onMessage(String message) {
            lastMessage = message;
            messageLatch.countDown();
        }

        boolean awaitOpen(long timeout, TimeUnit unit) throws InterruptedException {
            return openLatch.await(timeout, unit);
        }

        boolean awaitMessage(long timeout, TimeUnit unit) throws InterruptedException {
            return messageLatch.await(timeout, unit);
        }

        void resetMessageLatch() {
            messageLatch = new CountDownLatch(1);
        }

        int lastCode() {
            return readJsonInt("code");
        }

        String lastAction() {
            return readJsonString("action");
        }

        String lastDataTitle() {
            return readJsonStringFromData("title");
        }

        long lastDataMemberId() {
            return readJsonLongFromData();
        }

        private int readJsonInt(String field) {
            String pattern = "\"" + field + "\":";
            int start = lastMessage.indexOf(pattern);
            if (start < 0) {
                return -1;
            }
            start += pattern.length();
            int end = lastMessage.indexOf(',', start);
            if (end < 0) {
                end = lastMessage.indexOf('}', start);
            }
            return Integer.parseInt(lastMessage.substring(start, end).trim());
        }

        private String readJsonString(String field) {
            String pattern = "\"" + field + "\":\"";
            int start = lastMessage.indexOf(pattern);
            if (start < 0) {
                return null;
            }
            start += pattern.length();
            int end = lastMessage.indexOf('"', start);
            return lastMessage.substring(start, end);
        }

        private String readJsonStringFromData(String field) {
            String dataPattern = "\"data\":{";
            int dataStart = lastMessage.indexOf(dataPattern);
            if (dataStart < 0) {
                return null;
            }
            String fragment = lastMessage.substring(dataStart);
            String pattern = "\"" + field + "\":\"";
            int start = fragment.indexOf(pattern);
            if (start < 0) {
                return null;
            }
            start += pattern.length();
            int end = fragment.indexOf('"', start);
            return fragment.substring(start, end);
        }

        private long readJsonLongFromData() {
            String pattern = "\"data\":";
            int start = lastMessage.indexOf(pattern);
            if (start < 0) {
                return -1;
            }
            start += pattern.length();
            int end = start;
            while (end < lastMessage.length()) {
                char c = lastMessage.charAt(end);
                if (c == ',' || c == '}') {
                    break;
                }
                end++;
            }
            return Long.parseLong(lastMessage.substring(start, end).trim());
        }
    }

    static final class MapLikeNotice {
        private final String title;

        private MapLikeNotice(String title) {
            this.title = title;
        }

        static MapLikeNotice of(String key, String value) {
            if (!"title".equals(key)) {
                throw new IllegalArgumentException("only title supported in test stub");
            }
            return new MapLikeNotice(value);
        }

        public String getTitle() {
            return title;
        }
    }
}
