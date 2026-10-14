package com.cat2bug.admin;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class QuarkusReportImNoticeTest {

    @Test
    void reportCreateNotifiesMemberWithNewReportEventAndSkipsUnconfiguredMail() {
        String token = QuarkusTestAuth.loginAsAdmin();

        Map<String, Object> modules = Map.of(
                "defect", Map.of(
                        "event", Map.of("receiver", true, "collect", true),
                        "option", Map.of("realtime", Map.of("switch", true))),
                "report", Map.of(
                        "event", Map.of("new", true),
                        "option", Map.of("realtime", Map.of("switch", true))));
        Map<String, Object> platforms = Map.of(
                "asystem", Map.of("switch", true, "backgroundMusic", false, "panel", false),
                "bmail", Map.of("switch", true, "receiver", "admin@example.com"));
        given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body(Map.of("projectId", 1, "memberId", 1, "config", Map.of("modules", modules, "platforms", platforms)))
                .when().post("/im/config")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));

        String title = "IM报告通知-" + System.nanoTime();
        given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body(Map.of("reportTitle", title, "projectId", 1, "reportDescription", "d",
                        "srcHost", "http://localhost:2222"))
                .when().post("/system/report")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.reportId", notNullValue());

        List<String> titles = given()
                .header("Authorization", token)
                .queryParam("groupName", "report")
                .queryParam("pageSize", 50)
                .when().get("/system/notice/list")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .extract().path("rows.noticeTitle");
        org.hamcrest.MatcherAssert.assertThat(titles, hasItem("报告:" + title));
    }
}
