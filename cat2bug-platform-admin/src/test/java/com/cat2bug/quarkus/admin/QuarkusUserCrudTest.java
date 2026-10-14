package com.cat2bug.quarkus.admin;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * 用户管理写 API 集成测试：创建 → 更新 → 状态变更 → 删除。
 */
@QuarkusTest
class QuarkusUserCrudTest {

    private String token;

    @BeforeEach
    void login() {
        token = QuarkusTestAuth.loginAsAdmin();
    }

    @Test
    void userCrudLifecycle() {
        String username = "quarkus_crud_" + System.currentTimeMillis();

        given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"userName\":\"" + username + "\",\"nickName\":\"CRUD Test\","
                        + "\"password\":\"password123\",\"status\":\"0\",\"roleIds\":[2]}")
                .when().post("/system/user")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));

        Number userId = given()
                .header("Authorization", token)
                .when().get("/system/user/list?userName=" + username + "&pageNum=1&pageSize=10")
                .then()
                .statusCode(200)
                .body("rows[0].userName", equalTo(username))
                .extract().path("rows[0].userId");

        given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"userId\":" + userId + ",\"userName\":\"" + username + "\","
                        + "\"nickName\":\"CRUD Updated\",\"status\":\"0\",\"roleIds\":[2]}")
                .when().put("/system/user")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));

        given()
                .header("Authorization", token)
                .when().get("/system/user/" + userId)
                .then()
                .statusCode(200)
                .body("data.nickName", equalTo("CRUD Updated"));

        given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"userId\":" + userId + ",\"status\":\"1\"}")
                .when().put("/system/user/changeStatus")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));

        given()
                .header("Authorization", token)
                .when().get("/system/user/" + userId)
                .then()
                .statusCode(200)
                .body("data.status", equalTo("1"));

        given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"userId\":" + userId + ",\"status\":\"0\"}")
                .when().put("/system/user/changeStatus")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));

        given()
                .header("Authorization", token)
                .when().delete("/system/user/" + userId)
                .then()
                .statusCode(200)
                .body("code", equalTo(200));

        given()
                .header("Authorization", token)
                .when().get("/system/user/list?userName=" + username + "&pageNum=1&pageSize=10")
                .then()
                .statusCode(200)
                .body("total", equalTo(0));
    }

    @Test
    void adminUserCannotBeDeleted() {
        given()
                .header("Authorization", token)
                .when().delete("/system/user/1")
                .then()
                .statusCode(200)
                .body("code", equalTo(500))
                .body("msg", equalTo("当前用户不能删除"));
    }

    @Test
    void resetPwdAndAuthRole() {
        String username = "quarkus_pwd_" + System.currentTimeMillis();

        given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"userName\":\"" + username + "\",\"nickName\":\"Pwd Test\","
                        + "\"password\":\"password123\",\"status\":\"0\",\"roleIds\":[2]}")
                .when().post("/system/user")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));

        Number userId = given()
                .header("Authorization", token)
                .when().get("/system/user/list?userName=" + username)
                .then()
                .extract().path("rows[0].userId");

        given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"userId\":" + userId + ",\"password\":\"newpass123\"}")
                .when().put("/system/user/resetPwd")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));

        given()
                .header("Authorization", token)
                .when().get("/system/user/authRole/" + userId)
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("user", notNullValue())
                .body("roles", notNullValue());

        given()
                .header("Authorization", token)
                .when().put("/system/user/authRole?userId=" + userId + "&roleIds=2&roleIds=3")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));

        given()
                .header("Authorization", token)
                .when().delete("/system/user/" + userId)
                .then()
                .statusCode(200)
                .body("code", equalTo(200));
    }

    @Test
    void deptTreeReturnsData() {
        given()
                .header("Authorization", token)
                .when().get("/system/user/deptTree")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());
    }
}
