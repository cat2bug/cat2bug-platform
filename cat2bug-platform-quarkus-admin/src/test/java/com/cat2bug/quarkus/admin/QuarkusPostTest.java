package com.cat2bug.quarkus.admin;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class QuarkusPostTest {

    @Test
    void postCrudAndUserInfoIncludesPosts() {
        String token = QuarkusTestAuth.loginAsAdmin();

        Number postId = given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"postCode\":\"q_post\",\"postName\":\"Quarkus测试岗位\",\"postSort\":1,\"status\":\"0\"}")
                .when().post("/system/post")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.postId", notNullValue())
                .extract().path("data.postId");

        given()
                .header("Authorization", token)
                .when().get("/system/post/" + postId)
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.postName", equalTo("Quarkus测试岗位"));

        given()
                .header("Authorization", token)
                .when().get("/system/user/1")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("posts", notNullValue())
                .body("postIds", notNullValue());

        given()
                .header("Authorization", token)
                .when().delete("/system/post/" + postId)
                .then()
                .statusCode(200)
                .body("code", equalTo(200));
    }
}
