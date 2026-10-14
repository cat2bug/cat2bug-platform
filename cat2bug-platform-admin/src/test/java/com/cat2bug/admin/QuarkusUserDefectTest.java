package com.cat2bug.admin;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class QuarkusUserDefectTest {

    @Test
    void userDefectCollectAndCloseEditWindowEndpoints() {
        String token = QuarkusTestAuth.loginAsAdmin();

        Number defectId = given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"defectName\":\"用户缺陷收藏测试\",\"projectId\":1,\"handleBy\":[1]}")
                .when().post("/system/defect")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.defectId", notNullValue())
                .extract().path("data.defectId");

        given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"defectId\":" + defectId + ",\"userId\":1,\"collect\":1}")
                .when().put("/system/defect/" + defectId)
                .then()
                .statusCode(200)
                .body("code", equalTo(200));

        given()
                .header("Authorization", token)
                .when().post("/system/defect/" + defectId + "/close-edit-window")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));

        given()
                .header("Authorization", token)
                .when().post("/system/case/close-edit-window")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));

        given()
                .header("Authorization", token)
                .when().post("/system/report/close-edit-window")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));
    }
}
