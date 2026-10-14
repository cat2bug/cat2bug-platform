package com.cat2bug.admin;

import com.cat2bug.framework.security.open.ApiKeyConstants;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class QuarkusOpenApiDefectTest {

    private static final String API_NAME = "OpenAPI缺陷测试";

    private String adminToken;
    private String apiKey;
    private Number projectId;

    @BeforeEach
    void createProjectAndApiKey() {
        adminToken = QuarkusTestAuth.loginAsAdmin();
        String suffix = String.valueOf(System.currentTimeMillis());

        Number teamId = given()
                .header("Authorization", adminToken)
                .contentType(ContentType.JSON)
                .body("{\"teamName\":\"OpenAPI缺陷团队" + suffix + "\",\"teamKey\":\"tk" + System.nanoTime() + "\"}")
                .when().post("/system/team")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .extract().path("data.teamId");

        given()
                .header("Authorization", adminToken)
                .contentType(ContentType.JSON)
                .body("{\"teamId\":" + teamId + ",\"projectName\":\"OpenAPI缺陷项目" + suffix + "\",\"projectKey\":\"pk" + System.nanoTime() + "\"}")
                .when().post("/system/project")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));

        projectId = given()
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
    void openApiDefectRequiresApiKey() {
        given()
                .when().get("/api/defect")
                .then()
                .statusCode(401);
    }

    @Test
    void openApiDefectListCreateAndGet() {
        given()
                .header(ApiKeyConstants.HEADER_NAME, apiKey)
                .when().get("/api/defect?pageNum=1&pageSize=10")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("total", greaterThanOrEqualTo(0));

        Number defectNum = given()
                .header(ApiKeyConstants.HEADER_NAME, apiKey)
                .contentType(ContentType.JSON)
                .body("{\"defectName\":\"OpenAPI缺陷\",\"defectType\":\"BUG\",\"defectLevel\":\"serious\","
                        + "\"defectDescribe\":\"通过Open API创建\",\"handlerAccountList\":[\"admin\"]}")
                .when().post("/api/defect")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.defectNum", notNullValue())
                .body("data.defectName", equalTo("OpenAPI缺陷"))
                .extract().path("data.defectNum");

        given()
                .header(ApiKeyConstants.HEADER_NAME, apiKey)
                .when().get("/api/defect/" + defectNum)
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.defectNum", equalTo(defectNum.intValue()))
                .body("data.defectLevel", equalTo("SERIOUS"));

        given()
                .header(ApiKeyConstants.HEADER_NAME, apiKey)
                .when().get("/api/defect?pageNum=1&pageSize=10")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("total", greaterThanOrEqualTo(1));
    }

    @Test
    void openApiDefectListPagination() {
        createDefect("分页缺陷A-" + System.currentTimeMillis());
        createDefect("分页缺陷B-" + System.currentTimeMillis());

        given()
                .header(ApiKeyConstants.HEADER_NAME, apiKey)
                .when().get("/api/defect?pageNum=1&pageSize=1")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("total", greaterThanOrEqualTo(2))
                .body("rows", hasSize(1));

        given()
                .header(ApiKeyConstants.HEADER_NAME, apiKey)
                .when().get("/api/defect?pageNum=2&pageSize=1")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("total", greaterThanOrEqualTo(2))
                .body("rows", hasSize(1));
    }

    @Test
    void openApiDefectDeliverablePathEnrichment() {
        String parentName = "ParentDel-" + System.currentTimeMillis();
        String childName = "ChildDel-" + System.currentTimeMillis();
        String expectedPath = parentName + "/" + childName;

        given()
                .header(ApiKeyConstants.HEADER_NAME, apiKey)
                .contentType(ContentType.JSON)
                .body("{\"deliverableName\":\"" + parentName + "\"}")
                .when().post("/api/deliverable")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));

        given()
                .header(ApiKeyConstants.HEADER_NAME, apiKey)
                .contentType(ContentType.JSON)
                .body("{\"deliverableName\":\"" + childName + "\",\"parentDeliverablePath\":\"" + parentName + "\"}")
                .when().post("/api/deliverable")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));

        Number moduleId = given()
                .header("Authorization", adminToken)
                .when().get("/system/module/list?projectId=" + projectId + "&moduleName=" + childName)
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .extract().path("data.find { it.moduleName == '" + childName + "' }.moduleId");

        Number defectNum = createDefect("交付物缺陷-" + System.currentTimeMillis());

        Number defectId = given()
                .header("Authorization", adminToken)
                .when().get("/system/defect/list?projectId=" + projectId + "&pageNum=1&pageSize=50")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .extract().path("rows.find { it.projectNum == " + defectNum + " }.defectId");

        given()
                .header("Authorization", adminToken)
                .contentType(ContentType.JSON)
                .body("{\"defectId\":" + defectId + ",\"projectId\":" + projectId + ",\"moduleId\":" + moduleId + "}")
                .when().put("/system/defect")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));

        given()
                .header(ApiKeyConstants.HEADER_NAME, apiKey)
                .when().get("/api/defect/" + defectNum)
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.deliverableName", equalTo(expectedPath));

        given()
                .header(ApiKeyConstants.HEADER_NAME, apiKey)
                .when().get("/api/defect?defectNum=" + defectNum + "&pageNum=1&pageSize=10")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("rows[0].deliverableName", equalTo(expectedPath));
    }

    @Test
    void openApiDefectWorkflowLogType() {
        Number defectNum = createDefect("工作流日志-" + System.currentTimeMillis());

        given()
                .header(ApiKeyConstants.HEADER_NAME, apiKey)
                .contentType(ContentType.JSON)
                .body("{\"describe\":\"驳回原因\"}")
                .when().post("/api/defect/" + defectNum + "/reject")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));

        given()
                .header(ApiKeyConstants.HEADER_NAME, apiKey)
                .when().get("/api/defect/" + defectNum)
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.currentLog.defectLogState", equalTo("REJECTED"));
    }

    private Number createDefect(String defectName) {
        return given()
                .header(ApiKeyConstants.HEADER_NAME, apiKey)
                .contentType(ContentType.JSON)
                .body("{\"defectName\":\"" + defectName + "\",\"defectType\":\"BUG\",\"defectLevel\":\"serious\","
                        + "\"defectDescribe\":\"测试缺陷\",\"handlerAccountList\":[\"admin\"]}")
                .when().post("/api/defect")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .extract().path("data.defectNum");
    }
}
