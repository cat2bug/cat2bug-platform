package com.cat2bug.quarkus.admin;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class QuarkusDefectTabsTest {

    private String token;

    @BeforeEach
    void login() {
        token = QuarkusTestAuth.loginAsAdmin();
    }

    @Test
    void defectTabsCrudAndDefectConfig() {
        given()
                .header("Authorization", token)
                .when().get("/system/tabs/list?projectId=1&pageNum=1&pageSize=10")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("total", greaterThanOrEqualTo(0))
                .body("rows", notNullValue());

        Number tabId = given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"tabName\":\"Quarkus测试页签\",\"projectId\":1,\"userId\":1,\"tabSort\":10,"
                        + "\"config\":{\"params\":{},\"collect\":false,\"handleBy\":[1],\"rejectCount\":0}}")
                .when().post("/system/tabs")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.tabId", notNullValue())
                .body("data.tabName", equalTo("Quarkus测试页签"))
                .body("data.projectId", equalTo(1))
                .extract().path("data.tabId");

        given()
                .header("Authorization", token)
                .when().get("/system/tabs/" + tabId)
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.tabId", equalTo(tabId.intValue()))
                .body("data.tabName", equalTo("Quarkus测试页签"));

        given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"tabId\":" + tabId + ",\"tabName\":\"Quarkus测试页签-已更新\",\"projectId\":1,\"userId\":1,\"tabSort\":20}")
                .when().put("/system/tabs")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));

        given()
                .header("Authorization", token)
                .when().get("/system/tabs/" + tabId)
                .then()
                .statusCode(200)
                .body("data.tabName", equalTo("Quarkus测试页签-已更新"));

        given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("[{\"tabId\":" + tabId + ",\"tabSort\":99}]")
                .when().put("/system/tabs/sort")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));

        given()
                .header("Authorization", token)
                .when().get("/system/tabs/list?projectId=1&userId=1&pageNum=1&pageSize=10")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("total", greaterThanOrEqualTo(1));

        given()
                .header("Authorization", token)
                .when().get("/system/defect/config")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.types", notNullValue())
                .body("data.states", notNullValue())
                .body("data.tabs", notNullValue());

        given()
                .header("Authorization", token)
                .when().delete("/system/tabs/" + tabId)
                .then()
                .statusCode(200)
                .body("code", equalTo(200));

        given()
                .header("Authorization", token)
                .when().get("/system/tabs/" + tabId)
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", equalTo(null));
    }
}
