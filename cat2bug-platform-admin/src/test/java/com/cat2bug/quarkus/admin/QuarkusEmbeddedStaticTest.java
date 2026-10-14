package com.cat2bug.quarkus.admin;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

@QuarkusTest
class QuarkusEmbeddedStaticTest {

    @Test
    void indexHtmlIsServedFromClasspath() {
        given()
                .when().get("/index.html")
                .then()
                .statusCode(200)
                .body(containsString("embedded-static-test"));
    }
}
