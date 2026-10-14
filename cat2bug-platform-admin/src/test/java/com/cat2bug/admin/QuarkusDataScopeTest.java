package com.cat2bug.admin;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 角色数据范围集成测试：「本部门」角色只能看到、修改本部门的用户与部门，请求参数无法注入过滤片段。
 */
@QuarkusTest
class QuarkusDataScopeTest {

    private static final String PASSWORD = "Cat2bug@123";

    @Test
    void deptScopedRoleOnlySeesAndEditsOwnDept() {
        String admin = QuarkusTestAuth.loginAsAdmin();
        String suffix = String.valueOf(System.currentTimeMillis());
        List<Long> userIds = new ArrayList<>();
        Long roleId = null;
        Long deptA = null;
        Long deptB = null;
        try {
            deptA = createDept(admin, "数据范围A" + suffix);
            deptB = createDept(admin, "数据范围B" + suffix);
            roleId = createDeptScopedRole(admin, suffix);

            String nameA = "ds_a_" + suffix;
            String nameA2 = "ds_a2_" + suffix;
            String nameB = "ds_b_" + suffix;
            long userA = createUser(admin, nameA, deptA, roleId);
            userIds.add(userA);
            long userA2 = createUser(admin, nameA2, deptA, null);
            userIds.add(userA2);
            long userB = createUser(admin, nameB, deptB, null);
            userIds.add(userB);

            String token = login(nameA);

            List<String> visible = given()
                    .header("Authorization", token)
                    .when().get("/system/user/list?pageNum=1&pageSize=200")
                    .then().statusCode(200).body("code", equalTo(200))
                    .extract().jsonPath().getList("rows.userName", String.class);
            assertTrue(visible.contains(nameA));
            assertTrue(visible.contains(nameA2));
            assertFalse(visible.contains(nameB));
            assertFalse(visible.contains("admin"));

            List<String> forged = given()
                    .header("Authorization", token)
                    .queryParam("pageNum", 1)
                    .queryParam("pageSize", 200)
                    .queryParam("params[dataScope]", " OR 1=1")
                    .queryParam("dataScope", " OR 1=1")
                    .when().get("/system/user/list")
                    .then().statusCode(200).body("code", equalTo(200))
                    .extract().jsonPath().getList("rows.userName", String.class);
            assertEquals(visible.size(), forged.size());
            assertFalse(forged.contains(nameB));

            List<Long> depts = given()
                    .header("Authorization", token)
                    .queryParam("params[dataScope]", " OR 1=1")
                    .when().get("/system/dept/list")
                    .then().statusCode(200).body("code", equalTo(200))
                    .extract().jsonPath().getList("data.deptId", Long.class);
            assertEquals(List.of(deptA), depts);

            given()
                    .header("Authorization", token)
                    .contentType(ContentType.JSON)
                    .body(userJson(userB, nameB, deptB))
                    .when().put("/system/user")
                    .then()
                    .body("code", not(equalTo(200)))
                    .body("msg", containsString("没有权限访问用户数据"));

            given()
                    .header("Authorization", token)
                    .when().get("/system/user/" + userB)
                    .then()
                    .body("code", not(equalTo(200)))
                    .body("msg", containsString("没有权限访问用户数据"));

            given()
                    .header("Authorization", token)
                    .contentType(ContentType.JSON)
                    .body(userJson(userA2, nameA2, deptA))
                    .when().put("/system/user")
                    .then().statusCode(200)
                    .body("code", equalTo(200));
        } finally {
            if (!userIds.isEmpty()) {
                given().header("Authorization", admin)
                        .when().delete("/system/user/" + String.join(",", userIds.stream().map(String::valueOf).toList()));
            }
            if (roleId != null) {
                given().header("Authorization", admin).when().delete("/system/role/" + roleId);
            }
            if (deptA != null) {
                given().header("Authorization", admin).when().delete("/system/dept/" + deptA);
            }
            if (deptB != null) {
                given().header("Authorization", admin).when().delete("/system/dept/" + deptB);
            }
        }
    }

    private static String userJson(long userId, String userName, long deptId) {
        return "{\"userId\":" + userId + ",\"userName\":\"" + userName + "\",\"nickName\":\"" + userName
                + "-改\",\"deptId\":" + deptId + ",\"status\":\"0\"}";
    }

    private static long createDept(String token, String name) {
        given().header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"parentId\":0,\"deptName\":\"" + name + "\",\"orderNum\":99,\"status\":\"0\"}")
                .when().post("/system/dept")
                .then().statusCode(200).body("code", equalTo(200));
        return given().header("Authorization", token)
                .when().get("/system/dept/list")
                .then().statusCode(200).body("code", equalTo(200))
                .extract().jsonPath().getLong("data.find { it.deptName == '" + name + "' }.deptId");
    }

    private static long createDeptScopedRole(String token, String suffix) {
        String roleKey = "ds_role_" + suffix;
        given().header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"roleName\":\"数据范围本部门" + suffix + "\",\"roleKey\":\"" + roleKey
                        + "\",\"roleSort\":99,\"status\":\"0\",\"menuIds\":[1,100,1000,1002,103]}")
                .when().post("/system/role")
                .then().statusCode(200).body("code", equalTo(200));
        long roleId = given().header("Authorization", token)
                .when().get("/system/role/list?pageNum=1&pageSize=500")
                .then().statusCode(200).body("code", equalTo(200))
                .extract().jsonPath().getLong("rows.find { it.roleKey == '" + roleKey + "' }.roleId");
        given().header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"roleId\":" + roleId + ",\"dataScope\":\"3\",\"deptIds\":[]}")
                .when().put("/system/role/dataScope")
                .then().statusCode(200).body("code", equalTo(200));
        return roleId;
    }

    private static long createUser(String token, String userName, long deptId, Long roleId) {
        String roles = roleId == null ? "" : ",\"roleIds\":[" + roleId + "]";
        given().header("Authorization", token)
                .contentType(ContentType.JSON)
                .body("{\"userName\":\"" + userName + "\",\"nickName\":\"" + userName + "\",\"password\":\""
                        + PASSWORD + "\",\"deptId\":" + deptId + ",\"status\":\"0\"" + roles + "}")
                .when().post("/system/user")
                .then().statusCode(200).body("code", equalTo(200));
        return given().header("Authorization", token)
                .queryParam("userName", userName)
                .when().get("/system/user/list?pageNum=1&pageSize=10")
                .then().statusCode(200).body("code", equalTo(200))
                .extract().jsonPath().getLong("rows.find { it.userName == '" + userName + "' }.userId");
    }

    private static String login(String userName) {
        return given()
                .contentType(ContentType.JSON)
                .body("{\"username\":\"" + userName + "\",\"password\":\"" + PASSWORD + "\"}")
                .when().post("/login")
                .then().statusCode(200).body("code", equalTo(200))
                .extract().path("token");
    }
}
