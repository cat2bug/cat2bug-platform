package com.cat2bug.admin;

import com.cat2bug.framework.security.open.ApiKeyConstants;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class QuarkusOpenApiDeliverableTest {

    private static final String API_NAME = "OpenAPI交付物测试";
    private static final String DELIVERABLE_NAME = "测试交付物";

    private String adminToken;
    private String apiKey;

    @BeforeEach
    void createProjectAndApiKey() {
        adminToken = QuarkusTestAuth.loginAsAdmin();
        String suffix = String.valueOf(System.currentTimeMillis());

        Number teamId = given()
                .header("Authorization", adminToken)
                .contentType(ContentType.JSON)
                .body("{\"teamName\":\"交付物团队" + suffix + "\",\"teamKey\":\"tk" + System.nanoTime() + "\"}")
                .when().post("/system/team")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .extract().path("data.teamId");

        given()
                .header("Authorization", adminToken)
                .contentType(ContentType.JSON)
                .body("{\"teamId\":" + teamId + ",\"projectName\":\"交付物项目" + suffix + "\",\"projectKey\":\"pk" + System.nanoTime() + "\"}")
                .when().post("/system/project")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));

        Number projectId = given()
                .header("Authorization", adminToken)
                .when().get("/system/project/list?teamId=" + teamId + "&pageNum=1&pageSize=10")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("total", greaterThanOrEqualTo(1))
                .extract().path("rows[0].projectId");

        given()
                .header("Authorization", adminToken)
                .contentType(ContentType.JSON)
                .body("{\"apiName\":\"" + API_NAME + "\",\"projectId\":" + projectId + ",\"enabled\":true}")
                .when().post("/system/api")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));

        apiKey = given()
                .header("Authorization", adminToken)
                .when().get("/system/api/list?projectId=" + projectId + "&apiName=" + API_NAME + "&pageNum=1&pageSize=10")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .extract().path("rows[0].apiId");
    }

    @Test
    void openApiDeliverableCreateAndTree() {
        given()
                .header(ApiKeyConstants.HEADER_NAME, apiKey)
                .contentType(ContentType.JSON)
                .body("{\"deliverableName\":\"" + DELIVERABLE_NAME + "\",\"remark\":\"备注\"}")
                .when().post("/api/deliverable")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.deliverableName", equalTo(DELIVERABLE_NAME))
                .body("data.deliverablePath", equalTo(DELIVERABLE_NAME));

        given()
                .header(ApiKeyConstants.HEADER_NAME, apiKey)
                .when().get("/api/deliverable/tree")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.find { it.deliverableName == '" + DELIVERABLE_NAME + "' }.deliverablePath",
                        equalTo(DELIVERABLE_NAME))
                .body("data.find { it.deliverableName == '" + DELIVERABLE_NAME + "' }.childrenCount",
                        notNullValue());
    }
}
