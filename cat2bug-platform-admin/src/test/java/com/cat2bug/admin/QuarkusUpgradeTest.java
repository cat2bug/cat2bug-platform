package com.cat2bug.admin;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class QuarkusUpgradeTest {

    @Test
    void upgradePreflightIsPublicAndMatchesSpringShape() {
        given()
                .when().get("/upgrade/preflight")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("msg", notNullValue())
                .body("data.databaseType", equalTo("mysql"))
                .body("data.currentVersion", equalTo("1.1.0"))
                .body("data.targetVersion", notNullValue())
                .body("data.pendingMigrations", notNullValue())
                .body("data.diffs", notNullValue())
                .body("data.suggestedConfig", notNullValue())
                .body("data.backupDirectory", notNullValue())
                .body("data.defaultBackupFileName", notNullValue());
    }

    @Test
    void upgradeStatusReportsFlywayPendingProbe() {
        given()
                .when().get("/upgrade/status")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.upgradeRequired", equalTo(false))
                .body("data.pendingMigrations", notNullValue())
                .body("data.currentVersion", equalTo("1.1.0"))
                .body("data.installed", notNullValue());
    }

    @Test
    void upgradeSubmitRejectsWithoutUpgradeRequired() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"databaseType\":\"h2\"}")
                .when().post("/upgrade/submit")
                .then()
                .statusCode(200)
                .body("code", equalTo(500))
                .body("msg", equalTo("当前无需升级"));
    }

    @Test
    void upgradeSubmitRejectsWhenNoUpgradeRequired() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"databaseType\":\"h2\",\"adminUsername\":\"admin\",\"adminPassword\":\"cat2bug\"}")
                .when().post("/upgrade/submit")
                .then()
                .statusCode(200)
                .body("code", equalTo(500))
                .body("msg", equalTo("当前无需升级"));
    }

    @Test
    void upgradeRetryRequiresAdminCredentials() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"databaseType\":\"h2\"}")
                .when().post("/upgrade/retry")
                .then()
                .statusCode(200)
                .body("code", equalTo(500))
                .body("msg", equalTo("请提供系统管理员账号和密码"));
    }
}
