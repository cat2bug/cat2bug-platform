package com.cat2bug.admin;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class QuarkusAdminTest {

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
    void setupAndUpgradeStatusArePublic() {
        given()
                .when().get("/setup/status")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.installed", equalTo(false))
                .body("data.skipped", equalTo(false))
                .body("data.restartRequired", equalTo(false));

        given()
                .when().get("/upgrade/status")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.upgradeRequired", equalTo(false))
                .body("data.state", equalTo("completed"))
                .body("data.installed", equalTo(false));
    }

    @Test
    void rootIsPublicApiHint() {
        given()
                .accept(ContentType.JSON)
                .when().get("/")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));
    }

    @Test
    void loginGetInfoLogoutAndSessionInvalidation() {
        String token = given()
                .contentType(ContentType.JSON)
                .body("{\"username\":\"admin\",\"password\":\"cat2bug\"}")
                .when().post("/login")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("token", notNullValue())
                .extract().path("token");

        given()
                .header("Authorization", token)
                .when().get("/getInfo")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("user.userName", equalTo("admin"))
                .body("roles[0]", equalTo("admin"));

        given()
                .header("Authorization", token)
                .when().get("/getRouters")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.size()", org.hamcrest.Matchers.greaterThan(0));

        given()
                .header("Authorization", token)
                .when().get("/system/defect/list?projectId=1&pageNum=1&pageSize=10")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("total", org.hamcrest.Matchers.greaterThanOrEqualTo(0))
                .body("rows.size()", org.hamcrest.Matchers.lessThanOrEqualTo(10));

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

        String token2 = given()
                .contentType(ContentType.JSON)
                .body("{\"username\":\"admin\",\"password\":\"cat2bug\"}")
                .when().post("/login")
                .then()
                .statusCode(200)
                .extract().path("token");

        given()
                .header("Authorization", token2)
                .when().post("/logout")
                .then()
                .statusCode(200);

        given()
                .header("Authorization", token2)
                .when().get("/system/defect/list?projectId=1")
                .then()
                .statusCode(200)
                .body("code", equalTo(401));
    }

    @Test
    void captchaImageIsPublic() {
        given()
                .when().get("/captchaImage")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("captchaEnabled", notNullValue())
                .body("registerEnabled", notNullValue());
    }

    @Test
    void wechatMpLoginIsPublicNotUnauthorized() {
        // 未配小程序凭证时业务可能失败，但不得再被 JwtAuthFilter 打成 401
        given()
                .contentType(ContentType.JSON)
                .body("{\"code\":\"test\"}")
                .when().post("/wechat-mp/login")
                .then()
                .statusCode(200)
                .body("code", org.hamcrest.Matchers.not(equalTo(401)));
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
                .body("token", org.hamcrest.Matchers.nullValue());
    }
}
