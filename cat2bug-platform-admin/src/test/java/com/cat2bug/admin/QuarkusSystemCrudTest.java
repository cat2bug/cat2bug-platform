package com.cat2bug.admin;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Role / Menu / Dept CRUD 集成测试：创建后清理，并验证超级管理员角色受保护。
 */
@QuarkusTest
class QuarkusSystemCrudTest {

    @Test
    void deptRoleMenuCrudLifecycle() {
        String token = QuarkusTestAuth.loginAsAdmin();
        String suffix = String.valueOf(System.currentTimeMillis());

        given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"parentId\":0,\"deptName\":\"Quarkus测试部门" + suffix
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
                .extract().jsonPath().getLong("data.find { it.deptName == 'Quarkus测试部门" + suffix + "' }.deptId");

        given()
                .header("Authorization", token)
                .when().get("/system/dept/" + deptId)
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.deptName", equalTo("Quarkus测试部门" + suffix));

        given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"roleName\":\"Quarkus测试角色" + suffix + "\",\"roleKey\":\"quarkus_role_" + suffix
                        + "\",\"roleSort\":99,\"status\":\"0\",\"menuIds\":[]}")
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
                .extract().jsonPath().getLong("rows.find { it.roleKey == 'quarkus_role_" + suffix + "' }.roleId");

        given()
                .header("Authorization", token)
                .when().get("/system/role/optionselect")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.size()", greaterThanOrEqualTo(1));

        given()
                .header("Authorization", token)
                .when().get("/system/menu/roleMenuTreeselect/" + roleId)
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("checkedKeys", notNullValue())
                .body("menus", notNullValue());

        given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"parentId\":0,\"menuName\":\"Quarkus测试菜单" + suffix
                        + "\",\"orderNum\":99,\"path\":\"quarkus-test-" + suffix
                        + "\",\"component\":\"Layout\",\"menuType\":\"M\",\"isFrame\":\"1\",\"isCache\":\"0\","
                        + "\"visible\":\"0\",\"status\":\"0\"}")
                .when().post("/system/menu")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));

        Number menuId = given()
                .header("Authorization", token)
                .when().get("/system/menu/list")
                .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .extract().jsonPath().getLong("data.find { it.menuName == 'Quarkus测试菜单" + suffix + "' }.menuId");

        given()
                .header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"roleId\":" + roleId + ",\"status\":\"1\"}")
                .when().put("/system/role/changeStatus")
                .then()
                .statusCode(200)
                .body("code", equalTo(200));

        given()
                .header("Authorization", token)
                .when().delete("/system/menu/" + menuId)
                .then()
                .statusCode(200)
                .body("code", equalTo(200));

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

    @Test
    void adminRoleCannotBeDeleted() {
        String token = QuarkusTestAuth.loginAsAdmin();

        given()
                .header("Authorization", token)
                .when().delete("/system/role/1")
                .then()
                .statusCode(500)
                .body("code", equalTo(500))
                .body("msg", equalTo("不允许操作超级管理员角色"));
    }
}
