package com.cat2bug.admin;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;

/**
 * 团队/项目成员角色 API 集成测试。
 */
@QuarkusTest
class QuarkusTeamProjectRoleTest {

    private String token;

    @BeforeEach
    void login() {
        token = QuarkusTestAuth.loginAsAdmin();
    }

    @Test
    void teamRoleListReturnsTeamRoles() {
        String suffix = String.valueOf(System.currentTimeMillis());

        Number teamId = given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"teamName\":\"Quarkus角色测试团队" + suffix + "\",\"teamKey\":\"tk" + System.nanoTime() + "\"}")
                .when().post("/system/team")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.teamId", notNullValue())
                .extract().path("data.teamId");

        given()
                .header("Authorization", token)
                .when().get("/system/team/" + teamId + "/role")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.size()", greaterThanOrEqualTo(1))
                .body("data.roleKey", hasItem("team.admin"));
    }

    @Test
    void teamMemberRoleUpdateLifecycle() {
        String suffix = String.valueOf(System.currentTimeMillis());

        Number teamId = given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"teamName\":\"Quarkus成员角色团队" + suffix + "\",\"teamKey\":\"tk" + System.nanoTime() + "\"}")
                .when().post("/system/team")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .extract().path("data.teamId");

        given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("[4]")
                .when().put("/system/team/" + teamId + "/member/1/roles")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));

        given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"teamLock\":1}")
                .when().put("/system/team/" + teamId + "/member/1/role")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));

        given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"teamLock\":0}")
                .when().put("/system/team/" + teamId + "/member/1/role")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));
    }

    @Test
    void projectMemberRoleUpdateLifecycle() {
        String suffix = String.valueOf(System.currentTimeMillis());

        Number teamId = given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"teamName\":\"Quarkus项目角色团队" + suffix + "\",\"teamKey\":\"tk" + System.nanoTime() + "\"}")
                .when().post("/system/team")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .extract().path("data.teamId");

        given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"teamId\":" + teamId + ",\"projectName\":\"Quarkus角色测试项目" + suffix + "\",\"projectKey\":\"pk" + System.nanoTime() + "\"}")
                .when().post("/system/project")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));

        Number projectId = given()
                .header("Authorization", token)
                .when().get("/system/project/list?teamId=" + teamId + "&pageNum=1&pageSize=10")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .extract().jsonPath().getLong("rows.find { it.projectName == 'Quarkus角色测试项目" + suffix + "' }.projectId");

        given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("[6]")
                .when().put("/system/project/" + projectId + "/member/1/role")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));
    }

    @Test
    void projectMemberRoleRejectsEmptyRoles() {
        given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("[]")
                .when().put("/system/project/1/member/1/role")
                .then()
                .statusCode(500)
                .body("code", equalTo(500))
                .body("msg", equalTo("成员角色不能为空!"));
    }
}
