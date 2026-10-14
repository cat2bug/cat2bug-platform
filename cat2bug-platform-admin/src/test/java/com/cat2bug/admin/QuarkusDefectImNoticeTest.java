package com.cat2bug.admin;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class QuarkusDefectImNoticeTest {

    @Test
    void defectCreateAndAssignPersistNoticeForHandler() {
        String token = QuarkusTestAuth.loginAsAdmin();

        Number defectId = given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"defectName\":\"IM通知测试\",\"projectId\":1,\"handleBy\":[1]}")
                .when().post("/system/defect")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.defectId", notNullValue())
                .extract().path("data.defectId");

        given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"defectLogDescribe\":\"指派触发通知\",\"receiveBy\":[1],\"srcHost\":\"http://localhost:2222\"}")
                .when().post("/system/defect/" + defectId + "/assign")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));

        given()
                .header("Authorization", token)
                .queryParam("groupName", "defect")
                .when().get("/system/notice/list")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("total", greaterThanOrEqualTo(1));
    }
}
