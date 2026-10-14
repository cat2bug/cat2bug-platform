package com.cat2bug.quarkus.admin;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class QuarkusPhase3Test {

    @Test
    void dictAndConfigEndpointsAfterLogin() {
        String token = QuarkusTestAuth.loginAsAdmin();

        given()
                .header("Authorization", token)
                .when().get("/system/dict/data/type/sys_user_sex")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));

        given()
                .header("Authorization", token)
                .when().get("/system/config/configKey/sys.account.registerUser")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));

        given()
                .header("Authorization", token)
                .when().get("/system/config/list?pageNum=1&pageSize=10")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("total", greaterThanOrEqualTo(0));
    }

    @Test
    void dictDataCrudInvalidatesCache() {
        String token = QuarkusTestAuth.loginAsAdmin();
        String dictType = "quarkus_test_dict";

        given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"dictLabel\":\"原始\",\"dictValue\":\"0\",\"dictType\":\"" + dictType
                        + "\",\"dictSort\":99,\"status\":\"0\"}")
                .when().post("/system/dict/data")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));

        Number dictCode = given()
                .header("Authorization", token)
                .when().get("/system/dict/data/list?dictType=" + dictType + "&pageNum=1&pageSize=10")
                .then()
                .statusCode(200)
                .body("rows[0].dictCode", notNullValue())
                .extract().path("rows[0].dictCode");

        given()
                .header("Authorization", token)
                .when().get("/system/dict/data/type/" + dictType)
                .then()
                .statusCode(200)
                .body("data.dictLabel", hasItem("原始"));

        given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"dictCode\":" + dictCode + ",\"dictLabel\":\"已更新\",\"dictValue\":\"1\",\"dictType\":\""
                        + dictType + "\",\"dictSort\":99,\"status\":\"0\"}")
                .when().put("/system/dict/data")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));

        given()
                .header("Authorization", token)
                .when().get("/system/dict/data/type/" + dictType)
                .then()
                .statusCode(200)
                .body("data.dictLabel", hasItem("已更新"));

        given()
                .header("Authorization", token)
                .when().delete("/system/dict/data/" + dictCode)
                .then()
                .statusCode(200)
                .body("code", equalTo(200));
    }

    @Test
    void configCrudInvalidatesCache() {
        String token = QuarkusTestAuth.loginAsAdmin();
        String configKey = "quarkus.test.configKey";

        given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"configName\":\"Quarkus测试\",\"configKey\":\"" + configKey
                        + "\",\"configValue\":\"v1\",\"configType\":\"N\"}")
                .when().post("/system/config")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));

        given()
                .header("Authorization", token)
                .when().get("/system/config/configKey/" + configKey)
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", equalTo("v1"));

        Number configId = given()
                .header("Authorization", token)
                .when().get("/system/config/list?configKey=" + configKey + "&pageNum=1&pageSize=10")
                .then()
                .statusCode(200)
                .body("rows[0].configId", notNullValue())
                .extract().path("rows[0].configId");

        given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"configId\":" + configId + ",\"configName\":\"Quarkus测试\",\"configKey\":\""
                        + configKey + "\",\"configValue\":\"v2\",\"configType\":\"N\"}")
                .when().put("/system/config")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));

        given()
                .header("Authorization", token)
                .when().get("/system/config/configKey/" + configKey)
                .then()
                .statusCode(200)
                .body("data", equalTo("v2"));

        given()
                .header("Authorization", token)
                .when().delete("/system/config/" + configId)
                .then()
                .statusCode(200)
                .body("code", equalTo(200));
    }

    @Test
    void moduleListAndDefectState() {
        String token = QuarkusTestAuth.loginAsAdmin();
        given()
                .header("Authorization", token)
                .when().get("/system/module/list?projectId=1&modulePid=0")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());

        given()
                .header("Authorization", token)
                .when().get("/system/defect/state")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());
    }

    @Test
    void noticeGroupStatistics() {
        String token = QuarkusTestAuth.loginAsAdmin();
        given()
                .header("Authorization", token)
                .when().get("/system/notice/statistics/group")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("rows", notNullValue())
                .body("total", greaterThanOrEqualTo(0));
    }

    @Test
    void profileUploadIsPublic() {
        java.nio.file.Path uploadRoot = java.nio.file.Paths.get("./upload").toAbsolutePath().normalize();
        java.nio.file.Path sample = uploadRoot.resolve("upload");
        if (!java.nio.file.Files.isDirectory(sample)) {
            return;
        }
        try (var stream = java.nio.file.Files.list(sample)) {
            java.nio.file.Path first = stream.findFirst().orElse(null);
            if (first == null) {
                return;
            }
            String relative = uploadRoot.relativize(first).toString().replace('\\', '/');
            given()
                    .when().get("/profile/" + relative)
                    .then()
                    .statusCode(org.hamcrest.Matchers.anyOf(equalTo(200), equalTo(404)));
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void commentCrudLifecycle() {
        String token = QuarkusTestAuth.loginAsAdmin();
        long correlationId = 99_001L;
        String moduleType = "defect_log";

        given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"commentContent\":\"Quarkus 评论测试\",\"moduleType\":\"" + moduleType
                        + "\",\"correlationId\":" + correlationId + "}")
                .when().post("/system/comment")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));

        Number commentId = given()
                .header("Authorization", token)
                .when().get("/system/comment/list?moduleType=" + moduleType + "&correlationId=" + correlationId
                        + "&pageNum=1&pageSize=10")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("rows[0].commentId", notNullValue())
                .body("rows[0].commentContent", equalTo("Quarkus 评论测试"))
                .extract().path("rows[0].commentId");

        given()
                .header("Authorization", token)
                .when().get("/system/comment/" + commentId)
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.commentContent", equalTo("Quarkus 评论测试"));

        given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"commentId\":" + commentId + ",\"commentContent\":\"Quarkus 评论已更新\",\"moduleType\":\""
                        + moduleType + "\",\"correlationId\":" + correlationId + "}")
                .when().put("/system/comment")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));

        given()
                .header("Authorization", token)
                .when().get("/system/comment/" + commentId)
                .then()
                .statusCode(200)
                .body("data.commentContent", equalTo("Quarkus 评论已更新"));

        given()
                .header("Authorization", token)
                .when().delete("/system/comment/" + commentId)
                .then()
                .statusCode(200)
                .body("code", equalTo(200));
    }

    @Test
    void projectListByTeam() {
        String token = QuarkusTestAuth.loginAsAdmin();
        given()
                .header("Authorization", token)
                .when().get("/system/project/list?teamId=1&pageNum=1&pageSize=10")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("total", greaterThanOrEqualTo(0));
    }

    @Test
    void defectImportTemplateDownload() {
        String token = QuarkusTestAuth.loginAsAdmin();

        given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"currentProjectId\":1}")
                .when().put("/system/user-config")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));

        given()
                .header("Authorization", token)
                .contentType("application/x-www-form-urlencoded")
                .when().post("/system/defect/importTemplate")
                .then()
                .statusCode(200)
                .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    @Test
    void dashboardStatisticsEndpoints() {
        String token = QuarkusTestAuth.loginAsAdmin();

        given()
                .header("Authorization", token)
                .when().get("/system/dashboard/1/case")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.total", notNullValue());

        given()
                .header("Authorization", token)
                .when().get("/system/dashboard/1/defect")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.total", notNullValue());

        given()
                .header("Authorization", token)
                .when().get("/system/dashboard/1/defect-line?timeType=day")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.times", notNullValue())
                .body("data.types", notNullValue());

        given()
                .header("Authorization", token)
                .when().get("/system/dashboard/1/actions?type=all")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.actions", notNullValue());

        given()
                .header("Authorization", token)
                .when().get("/system/dashboard/1/plans")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("rows", notNullValue())
                .body("total", greaterThanOrEqualTo(0));

        given()
                .header("Authorization", token)
                .when().get("/system/dashboard/1/member-defect")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());

        given()
                .header("Authorization", token)
                .when().get("/system/dashboard/1/member-defect-line?timeType=day")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.time", notNullValue())
                .body("data.data", notNullValue());
    }
}
