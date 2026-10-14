package com.cat2bug.quarkus.admin;

import com.cat2bug.quarkus.common.domain.SysAiModuleConfig;
import com.cat2bug.quarkus.system.service.SysAiModuleConfigService;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
class QuarkusAiModuleConfigTest {

    private static final long PROJECT_ID = 9_900_001L;

    @Inject
    SysAiModuleConfigService sysAiModuleConfigService;

    private String token;

    @BeforeEach
    void login() {
        token = QuarkusTestAuth.loginAsAdmin();
        SysAiModuleConfig existing = sysAiModuleConfigService.selectSysAiModuleConfigByProjectId(PROJECT_ID);
        if (existing != null && existing.getAiId() != null) {
            sysAiModuleConfigService.deleteSysAiModuleConfigByAiId(existing.getAiId());
        }
    }

    @Test
    void aiModuleConfigCrudLifecycle() {
        given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"projectId\":" + PROJECT_ID + "}")
                .when().post("/system/ai")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));

        SysAiModuleConfig created = sysAiModuleConfigService.selectSysAiModuleConfigByProjectId(PROJECT_ID);
        assertNotNull(created);
        assertNotNull(created.getAiId());
        Number aiId = created.getAiId();

        given()
                .header("Authorization", token)
                .when().get("/system/ai/" + aiId)
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.aiId", equalTo(aiId.intValue()))
                .body("data.projectId", equalTo((int) PROJECT_ID))
                .body("data.createTime", notNullValue());

        given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"projectId\":" + PROJECT_ID + "}")
                .when().put("/system/ai")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));

        given()
                .header("Authorization", token)
                .when().get("/system/ai/" + aiId)
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.updateTime", notNullValue())
                .body("data.updateById", notNullValue());

        given()
                .header("Authorization", token)
                .when().delete("/system/ai/" + aiId)
                .then()
                .statusCode(200)
                .body("code", equalTo(200));

        given()
                .header("Authorization", token)
                .when().get("/system/ai/" + aiId)
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", equalTo(null));
    }
}
