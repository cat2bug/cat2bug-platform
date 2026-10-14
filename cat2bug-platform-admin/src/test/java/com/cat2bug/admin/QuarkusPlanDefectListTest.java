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
class QuarkusPlanDefectListTest {

    private String token;

    @BeforeEach
    void login() {
        token = QuarkusTestAuth.loginAsAdmin();
    }

    @Test
    void planDefectListReturnsTableDataInfo() {
        String planId = given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"planName\":\"缺陷列表测试计划\",\"planVersion\":\"1.0\",\"projectId\":1}")
                .when().post("/system/plan")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.planId", notNullValue())
                .extract().path("data.planId");

        given()
                .header("Authorization", token)
                .when().get("/system/plan/" + planId + "/defect/list?projectId=1&pageNum=1&pageSize=10")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("total", greaterThanOrEqualTo(0))
                .body("rows", notNullValue())
                .body("rows.size()", lessThanOrEqualTo(10));

        given()
                .header("Authorization", token)
                .when().delete("/system/plan/" + planId)
                .then()
                .statusCode(200)
                .body("code", equalTo(200));
    }
}
