package com.cat2bug.quarkus.admin;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class QuarkusReportHistoryCommentTest {

    private static final String DATE_TIME_PATTERN = "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}";

    private String token;

    @BeforeEach
    void login() {
        token = QuarkusTestAuth.loginAsAdmin();
    }

    @Test
    void reportEditHistoryLifecycle() {
        Number reportId = createReport("历史测试报告", "v1");
        try {
            assertHistorySize(reportId, 0);

            updateDescription(reportId, "v2");
            given()
                    .header("Authorization", token)
                    .when().get("/system/report/" + reportId + "/history")
                    .then()
                    .statusCode(200)
                    .body("code", equalTo(200))
                    .body("data", hasSize(1))
                    .body("data[0].reportId", equalTo(reportId))
                    .body("data[0].reportDescription", equalTo("v2"))
                    .body("data[0].createById", notNullValue())
                    .body("data[0].createMember.userId", notNullValue())
                    .body("data[0].createTime", matchesPattern(DATE_TIME_PATTERN));

            updateDescription(reportId, "v2");
            assertHistorySize(reportId, 1);

            given()
                    .header("Authorization", token)
                    .contentType(ContentType.JSON)
                    .body("{\"reportId\":" + reportId + ",\"reportTitle\":\"历史测试报告-改名\"}")
                    .when().put("/system/report")
                    .then()
                    .statusCode(200)
                    .body("code", equalTo(200));
            assertHistorySize(reportId, 1);

            updateDescription(reportId, "v3");
            List<Map<String, Object>> history = listHistory(reportId);
            assertEquals(2, history.size());
            assertEquals("v3", history.get(0).get("reportDescription"));
            Number v2HistoryId = (Number) history.get(1).get("historyId");

            given()
                    .header("Authorization", token)
                    .when().get("/system/report/history/" + v2HistoryId)
                    .then()
                    .statusCode(200)
                    .body("code", equalTo(200))
                    .body("data.historyId", equalTo(v2HistoryId))
                    .body("data.reportDescription", equalTo("v2"));

            given()
                    .header("Authorization", token)
                    .when().post("/system/report/history/" + v2HistoryId + "/restore")
                    .then()
                    .statusCode(200)
                    .body("code", equalTo(200));
            given()
                    .header("Authorization", token)
                    .when().get("/system/report/" + reportId)
                    .then()
                    .statusCode(200)
                    .body("data.reportDescription", equalTo("v2"));
            assertHistorySize(reportId, 3);

            given()
                    .header("Authorization", token)
                    .when().delete("/system/report/history/" + v2HistoryId)
                    .then()
                    .statusCode(200)
                    .body("code", equalTo(200));
            assertHistorySize(reportId, 2);
            given()
                    .header("Authorization", token)
                    .when().get("/system/report/history/" + v2HistoryId)
                    .then()
                    .statusCode(200)
                    .body("data", nullValue());

            given()
                    .header("Authorization", token)
                    .when().post("/system/report/history/999999999/restore")
                    .then()
                    .statusCode(200)
                    .body("code", equalTo(500));
        } finally {
            deleteReport(reportId);
        }
        assertHistorySize(reportId, 0);
    }

    @Test
    void reportAnnotationCommentLifecycle() {
        Number reportId = createReport("批注测试报告", "第一行\n第二行");
        try {
            given()
                    .header("Authorization", token)
                    .contentType(ContentType.JSON)
                    .body(Map.of(
                            "commentContent", "主批注",
                            "moduleType", "report",
                            "correlationId", reportId,
                            "anchorKind", "selection",
                            "quote", "第二行",
                            "contextBefore", "第一行\n",
                            "contextAfter", "",
                            "anchorHint", "h-1"))
                    .when().post("/system/comment")
                    .then()
                    .statusCode(200)
                    .body("code", equalTo(200));

            Map<String, Object> root = listReportComments(reportId).get(0);
            Number rootId = (Number) root.get("commentId");
            assertEquals("selection", root.get("anchorKind"));
            assertEquals("第二行", root.get("quote"));
            assertEquals("第一行\n", root.get("contextBefore"));
            assertEquals("h-1", root.get("anchorHint"));
            assertEquals(null, root.get("parentId"));

            given()
                    .header("Authorization", token)
                    .contentType(ContentType.JSON)
                    .body(Map.of("commentContent", "回复", "moduleType", "report",
                            "correlationId", reportId, "parentId", rootId))
                    .when().post("/system/comment")
                    .then()
                    .statusCode(200)
                    .body("code", equalTo(200));

            List<Map<String, Object>> comments = listReportComments(reportId);
            assertEquals(2, comments.size());
            Map<String, Object> reply = comments.stream()
                    .filter(c -> c.get("parentId") != null)
                    .findFirst()
                    .orElseThrow();
            assertEquals(rootId.longValue(), ((Number) reply.get("parentId")).longValue());

            given()
                    .header("Authorization", token)
                    .when().get("/system/comment/list?moduleType=report&correlationId=" + reportId
                            + "&parentId=" + rootId + "&pageNum=1&pageSize=1000")
                    .then()
                    .statusCode(200)
                    .body("total", equalTo(1))
                    .body("rows[0].commentContent", equalTo("回复"))
                    .body("rows[0].createTime", matchesPattern(DATE_TIME_PATTERN))
                    .body("rows[0].createMember.userId", notNullValue());

            postCommentExpectError(Map.of("commentContent", "楼中楼", "moduleType", "report",
                    "correlationId", reportId, "parentId", reply.get("commentId")));
            postCommentExpectError(Map.of("commentContent", "错锚点", "moduleType", "report",
                    "correlationId", reportId, "anchorKind", "word"));
            postCommentExpectError(Map.of("commentContent", "错关联", "moduleType", "report",
                    "correlationId", reportId.longValue() + 1, "parentId", rootId));
            postCommentExpectError(Map.of("commentContent", "错模块", "moduleType", "defect_log",
                    "correlationId", reportId, "parentId", rootId));
            postCommentExpectError(Map.of("commentContent", "父不存在", "moduleType", "report",
                    "correlationId", reportId, "parentId", 999999999L));

            given()
                    .header("Authorization", token)
                    .contentType(ContentType.JSON)
                    .body(Map.of("commentContent", "历史回复", "moduleType", "report_history",
                            "correlationId", reportId))
                    .when().post("/system/comment")
                    .then()
                    .statusCode(200)
                    .body("code", equalTo(200));
            Number historyCommentId = given()
                    .header("Authorization", token)
                    .when().get("/system/comment/list?moduleType=report_history&correlationId=" + reportId)
                    .then()
                    .statusCode(200)
                    .body("total", equalTo(1))
                    .extract().path("rows[0].commentId");

            given()
                    .header("Authorization", token)
                    .when().delete("/system/comment/" + rootId)
                    .then()
                    .statusCode(200)
                    .body("code", equalTo(200));
            assertEquals(0, listReportComments(reportId).size());

            given()
                    .header("Authorization", token)
                    .when().delete("/system/comment/" + historyCommentId)
                    .then()
                    .statusCode(200)
                    .body("code", equalTo(200));
        } finally {
            deleteReport(reportId);
        }
    }

    private Number createReport(String title, String description) {
        return given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body(Map.of("reportTitle", title, "projectId", 1, "reportDescription", description))
                .when().post("/system/report")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .extract().path("data.reportId");
    }

    private void deleteReport(Number reportId) {
        given()
                .header("Authorization", token)
                .when().delete("/system/report/" + reportId)
                .then()
                .statusCode(200)
                .body("code", equalTo(200));
    }

    private void updateDescription(Number reportId, String description) {
        given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body(Map.of("reportId", reportId, "reportDescription", description))
                .when().put("/system/report")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));
    }

    private List<Map<String, Object>> listHistory(Number reportId) {
        return given()
                .header("Authorization", token)
                .when().get("/system/report/" + reportId + "/history")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .extract().path("data");
    }

    private void assertHistorySize(Number reportId, int expected) {
        assertEquals(expected, listHistory(reportId).size());
    }

    private List<Map<String, Object>> listReportComments(Number reportId) {
        return given()
                .header("Authorization", token)
                .when().get("/system/comment/list?moduleType=report&correlationId=" + reportId
                        + "&pageNum=1&pageSize=1000")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .extract().path("rows");
    }

    private void postCommentExpectError(Map<String, Object> body) {
        given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body(body)
                .when().post("/system/comment")
                .then()
                .body("code", equalTo(500));
    }
}
