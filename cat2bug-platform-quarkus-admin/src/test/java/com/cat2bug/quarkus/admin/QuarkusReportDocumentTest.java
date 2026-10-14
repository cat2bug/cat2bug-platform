package com.cat2bug.quarkus.admin;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class QuarkusReportDocumentTest {

    private String token;

    @BeforeEach
    void login() {
        token = QuarkusTestAuth.loginAsAdmin();
    }

    @Test
    void reportListGetCrudLifecycle() {
        given()
                .header("Authorization", token)
                .when().get("/system/report/list?projectId=1&pageNum=1&pageSize=10")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("total", greaterThanOrEqualTo(0))
                .body("rows", notNullValue());

        Number reportId = given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"reportTitle\":\"Quarkus测试报告\",\"projectId\":1,\"reportDescription\":\"测试描述\"}")
                .when().post("/system/report")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.reportId", notNullValue())
                .body("data.reportTitle", equalTo("Quarkus测试报告"))
                .body("data.projectId", equalTo(1))
                .extract().path("data.reportId");

        given()
                .header("Authorization", token)
                .when().get("/system/report/" + reportId)
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.reportId", equalTo(reportId))
                .body("data.reportTitle", equalTo("Quarkus测试报告"));

        given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"reportId\":" + reportId + ",\"reportTitle\":\"Quarkus测试报告-已更新\",\"projectId\":1}")
                .when().put("/system/report")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));

        given()
                .header("Authorization", token)
                .when().get("/system/report/" + reportId)
                .then()
                .statusCode(200)
                .body("data.reportTitle", equalTo("Quarkus测试报告-已更新"));

        given()
                .header("Authorization", token)
                .when().get("/system/report/list?projectId=1&pageNum=1&pageSize=10")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("total", greaterThanOrEqualTo(1))
                .body("rows.size()", lessThanOrEqualTo(10));

        given()
                .header("Authorization", token)
                .when().delete("/system/report/" + reportId)
                .then()
                .statusCode(200)
                .body("code", equalTo(200));

        given()
                .header("Authorization", token)
                .when().get("/system/report/" + reportId)
                .then()
                .statusCode(500)
                .body("code", equalTo(500));
    }

    @Test
    void documentListGetCrudLifecycle() {
        given()
                .header("Authorization", token)
                .when().get("/system/document/list?projectId=1&pageNum=1&pageSize=10")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("total", greaterThanOrEqualTo(0))
                .body("rows", notNullValue());

        given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"docName\":\"Quarkus测试文档\",\"docType\":0,\"projectId\":1}")
                .when().post("/system/document")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));

        Number docId = given()
                .header("Authorization", token)
                .when().get("/system/document/list?projectId=1&docName=Quarkus测试文档&pageNum=1&pageSize=1")
                .then()
                .statusCode(200)
                .extract().path("rows[0].docId");

        given()
                .header("Authorization", token)
                .when().get("/system/document/" + docId)
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.docId", equalTo(docId))
                .body("data.docName", equalTo("Quarkus测试文档"));

        given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"docId\":" + docId + ",\"docName\":\"Quarkus测试文档-已更新\",\"docType\":0,\"projectId\":1}")
                .when().put("/system/document")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));

        given()
                .header("Authorization", token)
                .when().get("/system/document/" + docId)
                .then()
                .statusCode(200)
                .body("data.docName", equalTo("Quarkus测试文档-已更新"));

        given()
                .header("Authorization", token)
                .when().get("/system/document/list?projectId=1&pageNum=1&pageSize=10")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("total", greaterThanOrEqualTo(1))
                .body("rows.size()", lessThanOrEqualTo(10));

        given()
                .header("Authorization", token)
                .when().delete("/system/document/" + docId)
                .then()
                .statusCode(200)
                .body("code", equalTo(200));
    }
}
