package com.cat2bug.quarkus.admin;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class QuarkusCloudCaseTest {

    private String token;

    @BeforeEach
    void login() {
        token = QuarkusTestAuth.loginAsAdmin();
    }

    @Test
    void cloudCaseListRequiresAuth() {
        given()
                .when().get("/cloud/api/case/list")
                .then()
                .statusCode(200)
                .body("code", equalTo(401));
    }

    @Test
    void cloudCaseListReturnsArrayWhenAuthorized() {
        given()
                .header("Authorization", token)
                .when().get("/cloud/api/case/list?content=test")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());
    }
}
