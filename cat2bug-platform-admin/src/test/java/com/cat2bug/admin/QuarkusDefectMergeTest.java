package com.cat2bug.admin;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class QuarkusDefectMergeTest {

    private static final String PASSWORD = "Cat2bug@123";

    @Test
    void mergeDefectsSuccess() {
        String token = QuarkusTestAuth.loginAsAdmin();

        Number masterId = createDefect(token, "合并主缺陷");
        Number slaveId = createDefect(token, "合并从缺陷");

        Map<String, Object> master = given()
                .header("Authorization", token)
                .when().get("/system/defect/" + masterId)
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .extract().path("data");

        master.put("defectName", "合并后标题");

        given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "projectId", 1,
                        "masterDefectId", masterId,
                        "sourceDefectIds", java.util.List.of(masterId, slaveId),
                        "defect", master))
                .when().post("/system/defect/merge")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.defectId", equalTo(masterId.intValue()))
                .body("data.defectName", equalTo("合并后标题"));

        given()
                .header("Authorization", token)
                .when().get("/system/defect/list?pageNum=1&pageSize=50&projectId=1&params[delFlag]=2")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("rows.find { it.defectId == " + slaveId + " }.delFlag", equalTo("2"));

        given()
                .header("Authorization", token)
                .when().get("/system/log/list?defectId=" + masterId + "&defectLogType=MERGE&pageNum=1&pageSize=5")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("total", equalTo(1))
                .body("rows[0].defectLogType", equalTo("MERGE"));
    }

    @Test
    void mergeRejectsMasterNotInSources() {
        String token = QuarkusTestAuth.loginAsAdmin();
        Number masterId = createDefect(token, "主不在集合主");
        Number b = createDefect(token, "主不在集合B");
        Number c = createDefect(token, "主不在集合C");

        Map<String, Object> master = given()
                .header("Authorization", token)
                .when().get("/system/defect/" + masterId)
                .then().extract().path("data");
        master.put("defectName", "不应写入");

        given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "projectId", 1,
                        "masterDefectId", masterId,
                        "sourceDefectIds", java.util.List.of(b, c),
                        "defect", master))
                .when().post("/system/defect/merge")
                .then()
                .statusCode(500);

        given()
                .header("Authorization", token)
                .when().get("/system/defect/" + masterId)
                .then()
                .body("data.defectName", equalTo("主不在集合主"));
    }

    @Test
    void mergeRejectsDeletedSource() {
        String token = QuarkusTestAuth.loginAsAdmin();
        Number masterId = createDefect(token, "源已删主");
        Number slaveId = createDefect(token, "源已删从");

        given()
                .header("Authorization", token)
                .when().delete("/system/defect/" + slaveId)
                .then()
                .statusCode(200)
                .body("code", equalTo(200));

        Map<String, Object> master = given()
                .header("Authorization", token)
                .when().get("/system/defect/" + masterId)
                .then().extract().path("data");
        master.put("defectName", "不应写入");

        given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "projectId", 1,
                        "masterDefectId", masterId,
                        "sourceDefectIds", java.util.List.of(masterId, slaveId),
                        "defect", master))
                .when().post("/system/defect/merge")
                .then()
                .statusCode(500);

        given()
                .header("Authorization", token)
                .when().get("/system/defect/" + masterId)
                .then()
                .body("data.defectName", equalTo("源已删主"));
    }

    @Test
    void mergeRequiresPermission() {
        String admin = QuarkusTestAuth.loginAsAdmin();
        String userName = "merge_noperm_" + System.currentTimeMillis();
        given()
                .header("Authorization", admin)
                .contentType(ContentType.JSON)
                .body("{\"userName\":\"" + userName + "\",\"nickName\":\"" + userName + "\",\"password\":\""
                        + PASSWORD + "\",\"status\":\"0\"}")
                .when().post("/system/user")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));

        Long userId = given()
                .header("Authorization", admin)
                .queryParam("userName", userName)
                .when().get("/system/user/list?pageNum=1&pageSize=10")
                .then()
                .extract().jsonPath().getLong("rows.find { it.userName == '" + userName + "' }.userId");

        String token = given()
                .contentType(ContentType.JSON)
                .body("{\"username\":\"" + userName + "\",\"password\":\"" + PASSWORD + "\"}")
                .when().post("/login")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .extract().path("token");

        try {
            given()
                    .header("Authorization", token)
                    .contentType(ContentType.JSON)
                    .body("{\"projectId\":1,\"masterDefectId\":1,\"sourceDefectIds\":[1,2],\"defect\":{\"defectId\":1,\"projectId\":1}}")
                    .when().post("/system/defect/merge")
                    .then()
                    .statusCode(200)
                    .body("code", equalTo(403));
        } finally {
            given().header("Authorization", admin).when().delete("/system/user/" + userId);
        }
    }

    private static Number createDefect(String token, String name) {
        return given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"defectName\":\"" + name + "\",\"projectId\":1,\"handleBy\":[1]}")
                .when().post("/system/defect")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.defectId", notNullValue())
                .extract().path("data.defectId");
    }
}
