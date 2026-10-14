package com.cat2bug.admin;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class QuarkusAiApiTest {

    private String token;

    @BeforeEach
    void login() {
        token = QuarkusTestAuth.loginAsAdmin();
    }

    @Test
    void aiListReturnsEmptyWhenOllamaDisabled() {
        given()
                .header("Authorization", token)
                .when().get("/system/ai/list")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("total", equalTo(0))
                .body("rows", hasSize(0));
    }

    @Test
    void projectModelOptionsMatchesSpringShape() {
        given()
                .header("Authorization", token)
                .when().get("/system/ai/project-model-options?projectId=1")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.ollama", notNullValue())
                .body("data.ollama", hasSize(0))
                .body("data.openai", notNullValue())
                .body("data.openai", hasSize(0))
                .body("data.aiEnabled", equalTo(false));
    }

    @Test
    void defaultModelListReturnsConfiguredModels() {
        given()
                .header("Authorization", token)
                .when().get("/system/ai/model/list/default")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("total", equalTo(2))
                .body("rows", hasSize(2))
                .body("rows[0]", equalTo("llama3:8b"))
                .body("rows[1]", equalTo("llava:7b"));
    }

    @Test
    void downloadModelFailsWhenOllamaDisabled() {
        given()
                .header("Authorization", token)
                .contentType("application/json")
                .body("{\"name\":\"llama3:8b\"}")
                .when().post("/system/ai/model")
                .then()
                .statusCode(500)
                .body("code", equalTo(500))
                .body("msg", equalTo("Ollama service is not enabled"));
    }

    @Test
    void openAiCaseListRequiresAccountWhenEnabled() {
        given()
                .header("Authorization", token)
                .contentType("application/json")
                .body("{\"serviceType\":\"openai\",\"projectId\":1,\"prompt\":\"登录\",\"rowCount\":1}")
                .when().post("/ai/case/list")
                .then()
                .statusCode(200)
                .body("code", equalTo(500))
                .body("msg", equalTo("未找到可用的 AI 模型，请先配置 OpenAI 账号。"));
    }

    @Test
    void removeModelFailsWhenOllamaDisabled() {
        given()
                .header("Authorization", token)
                .contentType("application/json")
                .body("{\"name\":\"llama3:8b\"}")
                .when().delete("/system/ai/model")
                .then()
                .statusCode(500)
                .body("code", equalTo(500))
                .body("msg", equalTo("Ollama service is not enabled"));
    }
}
