package com.cat2bug.admin;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class QuarkusProjectApiTest {

    private static final String API_NAME = "Quarkus测试API";

    private String token;

    @BeforeEach
    void login() {
        token = QuarkusTestAuth.loginAsAdmin();
    }

    @Test
    void projectApiListGetCrudLifecycle() {
        given()
                .header("Authorization", token)
                .when().get("/system/api/list?projectId=1&pageNum=1&pageSize=10")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("total", greaterThanOrEqualTo(0))
                .body("rows", notNullValue());

        given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"apiName\":\"" + API_NAME + "\",\"projectId\":1,\"enabled\":true}")
                .when().post("/system/api")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));

        String apiId = given()
                .header("Authorization", token)
                .when().get("/system/api/list?projectId=1&apiName=" + API_NAME + "&pageNum=1&pageSize=10")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("total", greaterThanOrEqualTo(1))
                .body("rows[0].apiId", notNullValue())
                .body("rows[0].apiName", equalTo(API_NAME))
                .extract().path("rows[0].apiId");

        given()
                .header("Authorization", token)
                .when().get("/system/api/" + apiId)
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.apiId", equalTo(apiId))
                .body("data.apiName", equalTo(API_NAME));

        Number userId = given()
                .header("Authorization", token)
                .when().get("/system/api/" + apiId)
                .then()
                .statusCode(200)
                .extract().path("data.userId");

        given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"apiId\":\"" + apiId + "\",\"userId\":" + userId
                        + ",\"apiName\":\"Quarkus测试API-已更新\",\"projectId\":1,\"enabled\":true}")
                .when().put("/system/api")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));

        given()
                .header("Authorization", token)
                .when().get("/system/api/" + apiId)
                .then()
                .statusCode(200)
                .body("data.apiName", equalTo("Quarkus测试API-已更新"));

        given()
                .header("Authorization", token)
                .when().delete("/system/api/" + apiId)
                .then()
                .statusCode(200)
                .body("code", equalTo(200));

        given()
                .header("Authorization", token)
                .when().get("/system/api/" + apiId)
                .then()
                .statusCode(500)
                .body("code", equalTo(500));
    }
}
