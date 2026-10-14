package com.cat2bug.admin;

import io.restassured.http.ContentType;
import io.restassured.response.Response;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * 集成测试共用登录辅助。
 */
final class QuarkusTestAuth {

    private QuarkusTestAuth() {
    }

    static String loginAsAdmin() {
        return given()
                .contentType(ContentType.JSON)
                .body("{\"username\":\"admin\",\"password\":\"cat2bug\"}")
                .when().post("/login")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("token", notNullValue())
                .extract().path("token");
    }

    static void ensureCurrentProject(String token, long projectId) {
        given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"currentProjectId\":" + projectId + "}")
                .when().put("/system/user-config")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));
    }

    static void ensureProjectMember(String token, long projectId, long memberId) {
        if (isProjectMember(token, projectId, memberId)) {
            return;
        }
        Response response = given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"memberIds\":" + memberId + "}")
                .when().post("/system/project/" + projectId + "/member");
        int code = response.path("code");
        if (code == 200) {
            return;
        }
        String msg = response.path("msg");
        if (msg != null && msg.contains("Duplicate entry")) {
            return;
        }
        response.then()
                .statusCode(200)
                .body("code", equalTo(200));
    }

    static String projectMemberNickName(String token, long projectId, long memberId) {
        List<Map<String, Object>> rows = given()
                .header("Authorization", token)
                .queryParam("pageNum", 1)
                .queryParam("pageSize", 200)
                .when().get("/system/project/" + projectId + "/member")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .extract().path("rows");
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                Object userId = row.get("userId");
                if (userId instanceof Number number && number.longValue() == memberId) {
                    Object nickName = row.get("nickName");
                    if (nickName != null && !nickName.toString().isBlank()) {
                        return nickName.toString();
                    }
                }
            }
        }
        return "admin";
    }

    private static boolean isProjectMember(String token, long projectId, long memberId) {
        List<Map<String, Object>> rows = given()
                .header("Authorization", token)
                .queryParam("pageNum", 1)
                .queryParam("pageSize", 200)
                .when().get("/system/project/" + projectId + "/member")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .extract().path("rows");
        if (rows == null || rows.isEmpty()) {
            return false;
        }
        for (Map<String, Object> row : rows) {
            Object userId = row.get("userId");
            if (userId instanceof Number number && number.longValue() == memberId) {
                return true;
            }
        }
        return false;
    }
}
