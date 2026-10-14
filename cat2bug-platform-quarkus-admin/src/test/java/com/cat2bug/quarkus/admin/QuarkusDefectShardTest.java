package com.cat2bug.quarkus.admin;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class QuarkusDefectShardTest {

    private String token;

    @BeforeEach
    void login() {
        token = QuarkusTestAuth.loginAsAdmin();
    }

    @Test
    void defectShardCreateGetAndPublicView() {
        Number defectId = given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"defectName\":\"Quarkus缺陷分享测试\",\"projectId\":1,\"handleBy\":[1]}")
                .when().post("/system/defect")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.defectId", notNullValue())
                .extract().path("data.defectId");

        String defectShardId = given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"defectId\":" + defectId + ",\"agingHour\":24}")
                .when().post("/system/defect/" + defectId + "/shard")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.defectShardId", notNullValue())
                .body("data.defectId", equalTo(defectId.intValue()))
                .extract().path("data.defectShardId");

        given()
                .header("Authorization", token)
                .when().get("/system/defect/" + defectId + "/shard")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.defectShardId", equalTo(defectShardId))
                .body("data.defectId", equalTo(defectId.intValue()));

        given()
                .when().get("/system/defect/" + defectId + "/shard/" + defectShardId)
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.defect.defectId", equalTo(defectId.intValue()))
                .body("data.logs", notNullValue())
                .body("data.defectLevel", notNullValue());

        given()
                .header("Authorization", token)
                .when().get("/system/defect/" + defectId + "/shard/" + defectShardId)
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.redirect.projectId", notNullValue())
                .body("data.redirect.defectId", equalTo(defectId.intValue()));
    }
}
