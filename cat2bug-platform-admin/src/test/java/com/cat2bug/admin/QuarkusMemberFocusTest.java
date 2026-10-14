package com.cat2bug.admin;

import com.cat2bug.system.service.MemberFocusService;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
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

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class QuarkusMemberFocusTest {

    private static final long MEMBER_ID = 1L;

    @TestHTTPResource("/websocket/" + MEMBER_ID + "/message")
    URI websocketUri;

    @Inject
    MemberFocusService memberFocusService;

    @Test
    void getInfoSetsFocusListAndBroadcastsMemberFocus() throws Exception {
        String token = QuarkusTestAuth.loginAsAdmin();
        MemberFocusWebSocketClient client = new MemberFocusWebSocketClient();
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        Session session = container.connectToServer(client, websocketUri);
        try {
            assertTrue(client.awaitMessage(5, TimeUnit.SECONDS));
            client.resetMessageLatch();

            Number defectId = given()
                    .header("Authorization", token)
                    .contentType(ContentType.JSON)
                    .body("{\"defectName\":\"成员焦点测试\",\"projectId\":1,\"handleBy\":[1]}")
                    .when().post("/system/defect")
                    .then()
                    .statusCode(200)
                    .body("code", equalTo(200))
                    .extract().path("data.defectId");

            given()
                    .header("Authorization", token)
                    .when().get("/system/defect/" + defectId)
                    .then()
                    .statusCode(200)
                    .body("code", equalTo(200))
                    .body("data.focusList", notNullValue())
                    .body("data.focusList[0].userId", equalTo((int) MEMBER_ID));

            assertTrue(client.awaitMessage(5, TimeUnit.SECONDS));
            assertEquals("memberFocus", client.lastAction());
        } finally {
            session.close();
        }
    }

    @Test
    void listEnrichesFocusList() {
        String token = QuarkusTestAuth.loginAsAdmin();

        Number defectId = given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"defectName\":\"列表焦点测试\",\"projectId\":1,\"handleBy\":[1]}")
                .when().post("/system/defect")
                .then()
                .statusCode(200)
                .extract().path("data.defectId");

        given()
                .header("Authorization", token)
                .when().get("/system/defect/" + defectId)
                .then()
                .statusCode(200);

        given()
                .header("Authorization", token)
                .queryParam("projectId", 1)
                .when().get("/system/defect/list")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("rows.find { it.defectId == " + defectId + " }.focusList[0].userId",
                        equalTo((int) MEMBER_ID));
    }

    @Test
    void closeEditWindowRemovesFocus() {
        String token = QuarkusTestAuth.loginAsAdmin();

        Number defectId = given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"defectName\":\"关闭焦点测试\",\"projectId\":1,\"handleBy\":[1]}")
                .when().post("/system/defect")
                .then()
                .statusCode(200)
                .extract().path("data.defectId");

        given()
                .header("Authorization", token)
                .when().get("/system/defect/" + defectId)
                .then()
                .statusCode(200)
                .body("data.focusList.size()", equalTo(1));

        given()
                .header("Authorization", token)
                .when().post("/system/defect/" + defectId + "/close-edit-window")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));

        assertTrue(memberFocusService.getFocusMemberList("defect", defectId.longValue()).isEmpty());

        given()
                .header("Authorization", token)
                .queryParam("projectId", 1)
                .when().get("/system/defect/list")
                .then()
                .statusCode(200)
                .body("rows.find { it.defectId == " + defectId + " }.focusList.size()", equalTo(0));
    }

    @ClientEndpoint
    static class MemberFocusWebSocketClient {

        private CountDownLatch messageLatch = new CountDownLatch(1);
        private String lastMessage;

        @OnMessage
        public void onMessage(String message) {
            lastMessage = message;
            messageLatch.countDown();
        }

        boolean awaitMessage(long timeout, TimeUnit unit) throws InterruptedException {
            return messageLatch.await(timeout, unit);
        }

        void resetMessageLatch() {
            messageLatch = new CountDownLatch(1);
        }

        String lastAction() {
            String pattern = "\"action\":\"";
            int start = lastMessage.indexOf(pattern);
            if (start < 0) {
                return null;
            }
            start += pattern.length();
            int end = lastMessage.indexOf('"', start);
            return lastMessage.substring(start, end);
        }
    }
}
