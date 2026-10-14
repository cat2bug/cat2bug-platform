package com.cat2bug.admin;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class QuarkusDefectRestoreTest {

    @Test
    void deleteAndRestoreDefect() {
        String token = QuarkusTestAuth.loginAsAdmin();

        Number defectId = given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"defectName\":\"Quarkus恢复测试\",\"projectId\":1,\"handleBy\":[1]}")
                .when().post("/system/defect")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.defectId", notNullValue())
                .extract().path("data.defectId");

        given()
                .header("Authorization", token)
                .when().delete("/system/defect/" + defectId)
                .then()
                .statusCode(200)
                .body("code", equalTo(200));

        given()
                .header("Authorization", token)
                .when().get("/system/defect/" + defectId)
                .then()
                .statusCode(500)
                .body("code", equalTo(500));

        given()
                .header("Authorization", token)
                .when().put("/system/defect/" + defectId + "/restore")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));

        given()
                .header("Authorization", token)
                .when().get("/system/defect/" + defectId)
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.defectId", equalTo(defectId.intValue()));
    }
}
