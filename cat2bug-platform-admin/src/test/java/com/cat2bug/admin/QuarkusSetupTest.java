package com.cat2bug.admin;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class QuarkusSetupTest {

    @Test
    void setupEndpointsArePublic() {
        given()
                .when().get("/setup/status")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.installed", equalTo(false))
                .body("data.skipped", equalTo(false));
    }

    @Test
    void testPathWritable() throws Exception {
        Path tempDir = Files.createTempDirectory("cat2bug-setup-path-");
        given()
                .contentType(ContentType.JSON)
                .body("{\"path\":\"" + tempDir.toAbsolutePath().toString().replace("\\", "\\\\") + "\"}")
                .when().post("/setup/test/path")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.success", equalTo(true));
    }

    @Test
    void testDatabaseH2NewMode() {
        String dbName = "setup_test_" + UUID.randomUUID().toString().replace("-", "");
        given()
                .contentType(ContentType.JSON)
                .body("{\"databaseType\":\"h2\",\"database\":\"" + dbName + "\"}")
                .when().post("/setup/test/database")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.success", equalTo(true))
                .body("data.databaseMode", equalTo("new"));
    }

    @Test
    void testOllamaRejectsEmptyHost() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"host\":\"\"}")
                .when().post("/setup/test/ollama")
                .then()
                .statusCode(200)
                .body("code", equalTo(500))
                .body("msg", notNullValue());
    }
}
