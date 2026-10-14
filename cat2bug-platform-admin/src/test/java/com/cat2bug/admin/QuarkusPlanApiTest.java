package com.cat2bug.admin;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class QuarkusPlanApiTest {

    private String token;

    @BeforeEach
    void login() {
        token = QuarkusTestAuth.loginAsAdmin();
    }

    @Test
    void planListGetCrudLifecycle() {
        given()
                .header("Authorization", token)
                .when().get("/system/plan/list?projectId=1&pageNum=1&pageSize=10")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("total", greaterThanOrEqualTo(0))
                .body("rows", notNullValue());

        String planId = given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"planName\":\"Quarkus测试计划\",\"planVersion\":\"1.0\",\"projectId\":1}")
                .when().post("/system/plan")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.planId", notNullValue())
                .body("data.planName", equalTo("Quarkus测试计划"))
                .body("data.projectId", equalTo(1))
                .extract().path("data.planId");

        given()
                .header("Authorization", token)
                .when().get("/system/plan/" + planId)
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.planId", equalTo(planId))
                .body("data.planName", equalTo("Quarkus测试计划"));

        given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"planId\":\"" + planId + "\",\"planName\":\"Quarkus测试计划-已更新\",\"planVersion\":\"1.1\",\"projectId\":1}")
                .when().put("/system/plan")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));

        given()
                .header("Authorization", token)
                .when().get("/system/plan/" + planId)
                .then()
                .statusCode(200)
                .body("data.planName", equalTo("Quarkus测试计划-已更新"));

        given()
                .header("Authorization", token)
                .when().get("/system/plan/list?projectId=1&pageNum=1&pageSize=10")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("total", greaterThanOrEqualTo(1))
                .body("rows.size()", lessThanOrEqualTo(10));

        given()
                .header("Authorization", token)
                .when().delete("/system/plan/" + planId)
                .then()
                .statusCode(200)
                .body("code", equalTo(200));

        given()
                .header("Authorization", token)
                .when().get("/system/plan/" + planId)
                .then()
                .statusCode(500)
                .body("code", equalTo(500));
    }
}
