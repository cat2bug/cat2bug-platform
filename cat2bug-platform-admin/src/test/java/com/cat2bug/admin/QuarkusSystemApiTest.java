package com.cat2bug.admin;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * Phase 2.5/2.6：公开系统端点与登录流程集成测试。
 */
@QuarkusTest
class QuarkusSystemApiTest {

    @Test
    void versionIsPublicPlainText() {
        given()
                .when().get("/version")
                .then()
                .statusCode(200)
                .contentType("text/plain")
                .body(equalTo("1.1.0"));
    }

    @Test
    void setupStatusMatchesSpringShape() {
        given()
                .when().get("/setup/status")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("msg", notNullValue())
                .body("data.installed", equalTo(false))
                .body("data.skipped", equalTo(false))
                .body("data.restartRequired", equalTo(false));
    }

    @Test
    void upgradeStatusMatchesSpringShape() {
        given()
                .when().get("/upgrade/status")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("msg", notNullValue())
                .body("data.upgradeRequired", equalTo(false))
                .body("data.state", equalTo("completed"))
                .body("data.installed", equalTo(false))
                .body("data.skipped", equalTo(false))
                .body("data.restartRequired", equalTo(false))
                .body("data.pendingMigrations", notNullValue())
                .body("data.currentVersion", equalTo("1.1.0"));
    }

    @Test
    void protectedEndpointRequiresAuth() {
        given()
                .when().get("/getInfo")
                .then()
                .statusCode(200)
                .body("code", equalTo(401));
    }

    @Test
    void loginSuccessReturnsToken() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"username\":\"admin\",\"password\":\"cat2bug\"}")
                .when().post("/login")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("msg", notNullValue())
                .body("token", notNullValue());
    }

    @Test
    void wrongPasswordReturnsErrorWithoutToken() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"username\":\"admin\",\"password\":\"wrong\"}")
                .when().post("/login")
                .then()
                .statusCode(200)
                .body("code", equalTo(500))
                .body("token", nullValue());
    }

    @Test
    void registerIsPublicAndValidatesInput() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"username\":\"\",\"password\":\"password123\",\"nickName\":\"Test\"}")
                .when().post("/register")
                .then()
                .statusCode(200)
                .body("code", equalTo(500))
                .body("msg", equalTo("成员名不能为空"));

        given()
                .contentType(ContentType.JSON)
                .body("{\"username\":\"admin\",\"password\":\"password123\",\"nickName\":\"Admin\"}")
                .when().post("/register")
                .then()
                .statusCode(200)
                .body("code", equalTo(500))
                .body("msg", equalTo("保存成员[admin]失败，注册账号已存在"));
    }

    @Test
    void logoutInvalidatesSession() {
        String token = QuarkusTestAuth.loginAsAdmin();

        given()
                .header("Authorization", token)
                .when().get("/getInfo")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));

        given()
                .header("Authorization", token)
                .when().post("/logout")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));

        given()
                .header("Authorization", token)
                .when().get("/getInfo")
                .then()
                .statusCode(200)
                .body("code", equalTo(401));
    }
}
