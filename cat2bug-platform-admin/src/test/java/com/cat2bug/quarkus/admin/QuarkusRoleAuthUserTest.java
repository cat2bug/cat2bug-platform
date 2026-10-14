package com.cat2bug.quarkus.admin;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * 角色授权用户与数据权限集成测试。
 */
@QuarkusTest
class QuarkusRoleAuthUserTest {

    @Test
    void allocatedListAndDataScopeSmoke() {
        String token = QuarkusTestAuth.loginAsAdmin();
        String suffix = String.valueOf(System.currentTimeMillis());

        given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"parentId\":0,\"deptName\":\"Quarkus角色权限部门" + suffix
                        + "\",\"orderNum\":99,\"status\":\"0\"}")
                .when().post("/system/dept")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));

        Number deptId = given()
                .header("Authorization", token)
                .when().get("/system/dept/list")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .extract().jsonPath().getLong("data.find { it.deptName == 'Quarkus角色权限部门" + suffix + "' }.deptId");

        given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"roleName\":\"Quarkus角色权限测试" + suffix + "\",\"roleKey\":\"quarkus_auth_role_"
                        + suffix + "\",\"roleSort\":99,\"status\":\"0\",\"menuIds\":[]}")
                .when().post("/system/role")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));

        Number roleId = given()
                .header("Authorization", token)
                .when().get("/system/role/list?pageNum=1&pageSize=100")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .extract().jsonPath().getLong("rows.find { it.roleKey == 'quarkus_auth_role_" + suffix + "' }.roleId");

        given()
                .header("Authorization", token)
                .when().get("/system/role/authUser/allocatedList?roleId=" + roleId + "&pageNum=1&pageSize=10")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("total", greaterThanOrEqualTo(0));

        given()
                .header("Authorization", token)
                .when().get("/system/role/authUser/unallocatedList?roleId=" + roleId + "&pageNum=1&pageSize=10")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("total", greaterThanOrEqualTo(0));

        given()
                .header("Authorization", token)
                .when().get("/system/role/deptTree/" + roleId)
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("checkedKeys", notNullValue())
                .body("depts", notNullValue());

        given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"roleId\":" + roleId + ",\"dataScope\":\"2\",\"deptIds\":[" + deptId + "]}")
                .when().put("/system/role/dataScope")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));

        given()
                .header("Authorization", token)
                .when().get("/system/role/deptTree/" + roleId)
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("checkedKeys.size()", greaterThanOrEqualTo(1));

        given()
                .header("Authorization", token)
                .when().delete("/system/role/" + roleId)
                .then()
                .statusCode(200)
                .body("code", equalTo(200));

        given()
                .header("Authorization", token)
                .when().delete("/system/dept/" + deptId)
                .then()
                .statusCode(200)
                .body("code", equalTo(200));
    }
}
