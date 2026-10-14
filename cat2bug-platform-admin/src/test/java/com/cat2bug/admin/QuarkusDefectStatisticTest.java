package com.cat2bug.admin;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class QuarkusDefectStatisticTest {

    private String token;

    @BeforeEach
    void login() {
        token = QuarkusTestAuth.loginAsAdmin();
    }

    @Test
    void typeStateAndListEndpointsReturn200() {
        given()
                .header("Authorization", token)
                .when().get("/system/defect/statistic/type/1")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());

        given()
                .header("Authorization", token)
                .when().get("/system/defect/statistic/type/1/my")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());

        given()
                .header("Authorization", token)
                .when().get("/system/defect/statistic/state/1")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());

        given()
                .header("Authorization", token)
                .when().get("/system/defect/statistic/state/1/my")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());

        given()
                .header("Authorization", token)
                .when().get("/system/defect/statistic/module/1?pageNum=1&pageSize=10")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("total", greaterThanOrEqualTo(0))
                .body("rows", notNullValue());

        given()
                .header("Authorization", token)
                .when().get("/system/defect/statistic/open-workload/1?pageNum=1&pageSize=10")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("total", greaterThanOrEqualTo(0))
                .body("rows", notNullValue());

        given()
                .header("Authorization", token)
                .when().get("/system/defect/statistic/plan/list?projectId=1")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());

        given()
                .header("Authorization", token)
                .when().get("/system/defect/statistic/plan-metrics/1")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());

        given()
                .header("Authorization", token)
                .when().get("/system/defect/statistic/defect-state-line/1?timeType=day")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());

        given()
                .header("Authorization", token)
                .when().get("/system/defect/statistic/member-defect-line/1?timeType=day")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());
    }
}
