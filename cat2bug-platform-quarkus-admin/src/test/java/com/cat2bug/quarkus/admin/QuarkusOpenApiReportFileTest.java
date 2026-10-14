package com.cat2bug.quarkus.admin;

import com.cat2bug.quarkus.framework.security.open.ApiKeyConstants;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class QuarkusOpenApiReportFileTest {

    private static final String API_NAME = "OpenAPI报告文件测试";

    private String adminToken;
    private String apiKey;

    @BeforeEach
    void createProjectAndApiKey() {
        adminToken = QuarkusTestAuth.loginAsAdmin();
        String suffix = String.valueOf(System.currentTimeMillis());

        Number teamId = given()
                .header("Authorization", adminToken)
                .contentType(ContentType.JSON)
                .body("{\"teamName\":\"OpenAPI报告团队" + suffix + "\",\"teamKey\":\"tk" + System.nanoTime() + "\"}")
                .when().post("/system/team")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .extract().path("data.teamId");

        given()
                .header("Authorization", adminToken)
                .contentType(ContentType.JSON)
                .body("{\"teamId\":" + teamId + ",\"projectName\":\"OpenAPI报告项目" + suffix + "\",\"projectKey\":\"pk" + System.nanoTime() + "\"}")
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
    void openApiFileUploadRequiresApiKey() {
        given()
                .multiPart("file", "openapi-upload.txt", "hello".getBytes(StandardCharsets.UTF_8), "text/plain")
                .when().post("/api/file/upload")
                .then()
                .statusCode(401);
    }

    @Test
    void openApiFileUploadReturnsSpringFields() {
        given()
                .header(ApiKeyConstants.HEADER_NAME, apiKey)
                .multiPart("file", "openapi-upload.txt", "hello".getBytes(StandardCharsets.UTF_8), "text/plain")
                .when().post("/api/file/upload")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("url", notNullValue())
                .body("filePath", notNullValue())
                .body("serverFileName", notNullValue())
                .body("originalFilename", equalTo("openapi-upload.txt"))
                .body("fileExtension", equalTo("txt"));
    }

    @Test
    void openApiReportDefectRequiresApiKey() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"reportTitle\":\"测试报告\",\"reportData\":[]}")
                .when().post("/api/report/defect")
                .then()
                .statusCode(401);
    }

    @Test
    void openApiReportDefectAcceptsMinimalPayload() {
        given()
                .header(ApiKeyConstants.HEADER_NAME, apiKey)
                .contentType(ContentType.JSON)
                .body("{\"reportTitle\":\"OpenAPI测试报告\",\"reportDescription\":\"冒烟\",\"reportData\":[]}")
                .when().post("/api/report/defect")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));
    }

    @Test
    void openApiReportDefectCanInsertDefects() {
        given()
                .header(ApiKeyConstants.HEADER_NAME, apiKey)
                .contentType(ContentType.JSON)
                .body("{\"reportTitle\":\"带缺陷报告\",\"reportData\":["
                        + "{\"defectName\":\"报告缺陷\",\"defectDescribe\":\"通过报告推送创建\"}"
                        + "]}")
                .when().post("/api/report/defect")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));

        given()
                .header(ApiKeyConstants.HEADER_NAME, apiKey)
                .when().get("/api/defect")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("total", greaterThanOrEqualTo(1));
    }
}
