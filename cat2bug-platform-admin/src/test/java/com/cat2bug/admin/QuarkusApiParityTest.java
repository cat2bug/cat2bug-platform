package com.cat2bug.admin;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Phase 2.6 抽样：登录后核心 GET API 返回 200 且响应形态与 Spring 对齐（AjaxResult / TableDataInfo）。
 */
@QuarkusTest
class QuarkusApiParityTest {

    private String token;

    @BeforeEach
    void login() {
        token = QuarkusTestAuth.loginAsAdmin();
    }

    @Test
    void getInfoAndRoutersShape() {
        given()
                .header("Authorization", token)
                .when().get("/getInfo")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("msg", notNullValue())
                .body("user.userName", equalTo("admin"))
                .body("roles", notNullValue())
                .body("permissions", notNullValue());

        given()
                .header("Authorization", token)
                .when().get("/getRouters")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue())
                .body("data.size()", greaterThan(0));
    }

    @Test
    void systemUserRoleDeptMenuListShape() {
        given()
                .header("Authorization", token)
                .when().get("/system/user/list?pageNum=1&pageSize=10")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("msg", notNullValue())
                .body("total", greaterThanOrEqualTo(1))
                .body("rows", notNullValue());

        given()
                .header("Authorization", token)
                .when().get("/system/role/list?pageNum=1&pageSize=10")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("total", greaterThanOrEqualTo(0))
                .body("rows", notNullValue());

        given()
                .header("Authorization", token)
                .when().get("/system/dept/list")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());

        given()
                .header("Authorization", token)
                .when().get("/system/menu/list")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());
    }

    @Test
    void teamAndProjectListShape() {
        given()
                .header("Authorization", token)
                .when().get("/system/team/my?pageNum=1&pageSize=10")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("total", greaterThanOrEqualTo(0))
                .body("rows", notNullValue());

        given()
                .header("Authorization", token)
                .when().get("/system/project/list?teamId=1&pageNum=1&pageSize=10")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("total", greaterThanOrEqualTo(0))
                .body("rows", notNullValue());
    }

    @Test
    void defectAndCaseListShape() {
        given()
                .header("Authorization", token)
                .when().get("/system/defect/config")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.types", notNullValue())
                .body("data.states", notNullValue());

        given()
                .header("Authorization", token)
                .when().get("/system/defect/list?projectId=1&pageNum=1&pageSize=10")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("total", greaterThanOrEqualTo(0))
                .body("rows", notNullValue())
                .body("rows.size()", lessThanOrEqualTo(10));

        given()
                .header("Authorization", token)
                .when().get("/system/case/list?projectId=1&pageNum=1&pageSize=10")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("total", greaterThanOrEqualTo(0))
                .body("rows", notNullValue())
                .body("rows.size()", lessThanOrEqualTo(10));

        given()
                .header("Authorization", token)
                .contentType("application/json")
                .body("{\"currentProjectId\":1}")
                .when().put("/system/user-config")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));

        given()
                .header("Authorization", token)
                .when().get("/system/case/module/1/ids")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("rows", notNullValue());

        given()
                .header("Authorization", token)
                .when().get("/system/case/level/1/ids")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("rows", notNullValue());

        given()
                .header("Authorization", token)
                .when().get("/system/plan/list?projectId=1&pageNum=1&pageSize=10")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("total", greaterThanOrEqualTo(0))
                .body("rows", notNullValue())
                .body("rows.size()", lessThanOrEqualTo(10));
    }

    @Test
    void userConfigPartialUpdate() {
        given()
                .header("Authorization", token)
                .contentType("application/json")
                .body("{\"currentProjectId\":0}")
                .when().put("/system/user-config")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));
    }

    @Test
    void projectRoleAndStatisticTemplateList() {
        given()
                .header("Authorization", token)
                .when().get("/system/project/1/role?pageNum=1&pageSize=10")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("rows", notNullValue());

        given()
                .header("Authorization", token)
                .when().get("/system/statistic/list?userId=1&projectId=1&moduleType=1&pageNum=1&pageSize=10")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("rows", notNullValue());
    }
}
