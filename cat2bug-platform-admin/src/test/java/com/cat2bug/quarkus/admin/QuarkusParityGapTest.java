package com.cat2bug.quarkus.admin;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class QuarkusParityGapTest {

    private String token;

    @BeforeEach
    void login() {
        token = QuarkusTestAuth.loginAsAdmin();
    }

    @Test
    void defectLogCrud() {
        Number logId = given().header("Authorization", token)
                .contentType("application/json")
                .body(Map.of("defectId", 1, "defectLogDescribe", "parity-log", "defectLogType", "CREATE"))
                .when().post("/system/log")
                .then().statusCode(200).body("code", equalTo(200))
                .extract().path("data.defectLogId");
        Number id = logId != null ? logId : given().header("Authorization", token)
                .when().get("/system/log/list?defectId=1&pageNum=1&pageSize=200")
                .then().statusCode(200)
                .extract().path("rows.find { it.defectLogDescribe == 'parity-log' }.defectLogId");

        given().header("Authorization", token)
                .contentType("application/json")
                .body(Map.of("defectLogId", id, "defectLogDescribe", "parity-log-edited"))
                .when().put("/system/log")
                .then().statusCode(200).body("code", equalTo(200));
        given().header("Authorization", token)
                .when().get("/system/log/" + id)
                .then().statusCode(200).body("data.defectLogDescribe", equalTo("parity-log-edited"));
        given().header("Authorization", token)
                .when().delete("/system/log/" + id)
                .then().statusCode(200).body("code", equalTo(200));
    }

    @Test
    void userConfigAddAndOwnedDelete() {
        // 管理员替不存在的用户建配置，避免给真实账号产生第二条配置
        Number configId = given().header("Authorization", token)
                .contentType("application/json")
                .body(Map.of("userId", 987654321, "lifeContent", "{}"))
                .when().post("/system/user-config")
                .then().statusCode(200).body("code", equalTo(200))
                .body("data.userId", equalTo(987654321))
                .extract().path("data.userConfigId");
        given().header("Authorization", token)
                .when().delete("/system/user-config/" + configId)
                .then().statusCode(200).body("code", equalTo(200));
        given().header("Authorization", token)
                .when().delete("/system/user-config/" + configId)
                .then().statusCode(200).body("code", equalTo(500));
    }

    @Test
    void multiFileUpload() {
        given().header("Authorization", token)
                .multiPart("files", "a.txt", "hello".getBytes(), "text/plain")
                .multiPart("files", "b.txt", "world".getBytes(), "text/plain")
                .when().post("/common/uploads")
                .then().statusCode(200)
                .body("code", equalTo(200))
                .body("originalFilenames", equalTo("a.txt,b.txt"))
                .body("urls", containsString(","));
    }

    @Test
    void setupPreflightRejectedAfterInstall() {
        boolean installed = given().when().get("/setup/status")
                .then().statusCode(200).extract().path("data.installed");
        if (installed) {
            given().when().get("/setup/preflight")
                    .then().statusCode(200).body("code", equalTo(500));
        } else {
            given().when().get("/setup/preflight")
                    .then().statusCode(200).body("code", equalTo(200)).body("data", notNullValue());
        }
    }
}
