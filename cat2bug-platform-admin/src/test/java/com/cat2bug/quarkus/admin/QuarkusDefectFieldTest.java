package com.cat2bug.quarkus.admin;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class QuarkusDefectFieldTest {

    private String token;

    @BeforeEach
    void login() {
        token = QuarkusTestAuth.loginAsAdmin();
    }

    @Test
    void defectFieldEnabledListAndCrudLifecycle() {
        String suffix = String.format("%03d", System.currentTimeMillis() % 1000);
        String fieldKey = "q_sev_" + System.currentTimeMillis() % 100000;
        String fieldLabel = "测" + suffix;
        String updatedFieldLabel = "试" + suffix;
        given()
                .header("Authorization", token)
                .when().get("/system/project/1/defect-fields/enabled")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());

        Number fieldId = given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"fieldKey\":\"" + fieldKey + "\",\"fieldLabel\":\"" + fieldLabel + "\",\"fieldType\":\"enum\","
                        + "\"enabled\":1,\"typeConfig\":{\"options\":[{\"key\":\"p0\",\"label\":\"P0\"}]}}")
                .when().post("/system/project/1/defect-fields")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.fieldId", notNullValue())
                .body("data.fieldKey", equalTo(fieldKey))
                .body("data.fieldLabel", equalTo(fieldLabel))
                .body("data.projectId", equalTo(1))
                .extract().path("data.fieldId");

        given()
                .header("Authorization", token)
                .when().get("/system/project/1/defect-fields/enabled")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.size()", greaterThanOrEqualTo(1));

        given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"fieldLabel\":\"" + updatedFieldLabel + "\",\"fieldType\":\"enum\","
                        + "\"enabled\":1,\"typeConfig\":{\"options\":[{\"key\":\"p0\",\"label\":\"P0\"},"
                        + "{\"key\":\"p1\",\"label\":\"P1\"}]}}")
                .when().put("/system/project/1/defect-fields/" + fieldId)
                .then()
                .statusCode(200)
                .body("code", equalTo(200));

        given()
                .header("Authorization", token)
                .when().get("/system/project/1/defect-fields")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());

        given()
                .header("Authorization", token)
                .when().delete("/system/project/1/defect-fields/" + fieldId)
                .then()
                .statusCode(200)
                .body("code", equalTo(200));
    }
}
