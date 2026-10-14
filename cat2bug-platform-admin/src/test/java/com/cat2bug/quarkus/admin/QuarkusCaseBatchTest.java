package com.cat2bug.quarkus.admin;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

@QuarkusTest
class QuarkusCaseBatchTest {

    @Test
    void caseBatchInsert() {
        String token = QuarkusTestAuth.loginAsAdmin();
        String suffix = String.valueOf(System.currentTimeMillis() % 100000);

        given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("[{\"projectId\":1,\"caseName\":\"批量用例" + suffix + "\",\"caseType\":1,\"caseLevel\":1}]")
                .when().post("/system/case/batch")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.size()", greaterThanOrEqualTo(1));
    }
}
