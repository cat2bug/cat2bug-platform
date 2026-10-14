package com.cat2bug.admin;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class QuarkusDefectLogTest {

    private String token;

    @BeforeEach
    void login() {
        token = QuarkusTestAuth.loginAsAdmin();
    }

    @Test
    void defectLogListAndGetById() {
        Number defectId = given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"defectName\":\"Quarkus缺陷日志测试\",\"projectId\":1,\"handleBy\":[1]}")
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
                .body("code", equalTo(200));

        Number defectLogId = given()
                .header("Authorization", token)
                .when().get("/system/log/list?defectId=" + defectId + "&pageNum=1&pageSize=1")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("total", greaterThanOrEqualTo(1))
                .body("rows[0].defectLogId", notNullValue())
                .body("rows[0].defectId", equalTo(defectId.intValue()))
                .body("rows[0].createBy", notNullValue())
                .extract().path("rows[0].defectLogId");

        given()
                .header("Authorization", token)
                .when().get("/system/log/list?defectId=" + defectId + "&pageNum=1&pageSize=10")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("total", greaterThanOrEqualTo(1))
                .body("rows", notNullValue());

        given()
                .header("Authorization", token)
                .when().get("/system/log/" + defectLogId)
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.defectLogId", equalTo(defectLogId.intValue()))
                .body("data.defectId", equalTo(defectId.intValue()));
    }

    @Test
    void createWritesLogAndListCarriesCommentsAndReceivers() {
        Number defectId = given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"defectName\":\"Quarkus创建日志测试\",\"projectId\":1,\"handleBy\":[1]}")
                .when().post("/system/defect")
                .then()
                .statusCode(200)
                .extract().path("data.defectId");

        Number createLogId = given()
                .header("Authorization", token)
                .when().get("/system/log/list?defectId=" + defectId + "&pageNum=1&pageSize=10")
                .then()
                .statusCode(200)
                .body("total", equalTo(1))
                .body("rows[0].defectLogType", equalTo("CREATE"))
                .body("rows[0].receiveByList[0].userId", equalTo(1))
                .body("rows[0].commentList", hasSize(0))
                .extract().path("rows[0].defectLogId");

        given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"commentContent\":\"日志评论\",\"moduleType\":\"defect_log\",\"correlationId\":" + createLogId + "}")
                .when().post("/system/comment")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));

        given()
                .header("Authorization", token)
                .when().get("/system/log/list?defectId=" + defectId + "&pageNum=1&pageSize=10")
                .then()
                .statusCode(200)
                .body("rows[0].commentList", hasSize(1))
                .body("rows[0].commentList[0].commentContent", equalTo("日志评论"))
                .body("rows[0].commentList[0].createMember.userId", equalTo(1));

        given()
                .header("Authorization", token)
                .when().get("/system/log/" + createLogId)
                .then()
                .statusCode(200)
                .body("data.commentList", hasSize(1));
    }
}
