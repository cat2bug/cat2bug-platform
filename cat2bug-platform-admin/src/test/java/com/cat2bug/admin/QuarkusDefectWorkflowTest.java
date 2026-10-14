package com.cat2bug.admin;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class QuarkusDefectWorkflowTest {

    @Test
    void defectWorkflowEndpoints() {
        String token = QuarkusTestAuth.loginAsAdmin();

        Number defectId = given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"defectName\":\"Quarkus工作流测试\",\"projectId\":1,\"handleBy\":[1]}")
                .when().post("/system/defect")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.defectId", notNullValue())
                .extract().path("data.defectId");

        given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"defectLogDescribe\":\"指派给管理员\",\"receiveBy\":[1]}")
                .when().post("/system/defect/" + defectId + "/assign")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.defectLogType", equalTo("ASSIGN"));

        given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"defectLogDescribe\":\"提交修复\",\"receiveBy\":[1]}")
                .when().post("/system/defect/" + defectId + "/repair")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.defectLogType", equalTo("REPAIR"));

        given()
                .header("Authorization", token)
                .when().get("/system/defect/" + defectId)
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.defectState", equalTo(1));

        given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"defectLogDescribe\":\"驳回\",\"receiveBy\":[1]}")
                .when().post("/system/defect/" + defectId + "/reject")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.defectLogType", equalTo("REJECTED"));

        given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"defectLogDescribe\":\"重新打开\",\"receiveBy\":[1]}")
                .when().post("/system/defect/" + defectId + "/open")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.defectLogType", equalTo("OPEN"));

        given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"defectLogDescribe\":\"再次修复\",\"receiveBy\":[1]}")
                .when().post("/system/defect/" + defectId + "/repair")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.defectLogType", equalTo("REPAIR"));

        given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"defectLogDescribe\":\"验证通过\"}")
                .when().post("/system/defect/" + defectId + "/pass")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.defectLogType", equalTo("PASS"));

        given()
                .header("Authorization", token)
                .when().get("/system/defect/" + defectId)
                .then()
                .statusCode(200)
                .body("data.defectState", equalTo(4));

        given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"defectLogDescribe\":\"重新开启\"}")
                .when().post("/system/defect/" + defectId + "/open")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.defectLogType", equalTo("OPEN"));

        given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"defectLogDescribe\":\"直接关闭\"}")
                .when().post("/system/defect/" + defectId + "/close")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.defectLogType", equalTo("CLOSED"));

        given()
                .header("Authorization", token)
                .when().post("/system/defect/" + defectId + "/close-edit-window")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));
    }
}
