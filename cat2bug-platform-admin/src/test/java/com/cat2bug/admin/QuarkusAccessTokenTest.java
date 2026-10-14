package com.cat2bug.admin;

import com.cat2bug.common.domain.SysUserAccessToken;
import com.cat2bug.common.security.AccessTokens;
import com.cat2bug.system.mapper.SysUserAccessTokenMapper;
import com.cat2bug.system.service.SysUserAccessTokenService;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class QuarkusAccessTokenTest {

    private static final String PASSWORD = "Cat2bug@123";

    @Inject
    SysUserAccessTokenMapper tokenMapper;

    @Inject
    SysUserAccessTokenService tokenService;

    @Test
    void personalTokenLifecycle() {
        String admin = QuarkusTestAuth.loginAsAdmin();
        Response created = createToken(admin, "Cursor 测试", 90);
        String plaintext = created.path("data.token");
        Number tokenId = created.path("data.info.tokenId");
        assertTrue(plaintext.startsWith("c2b_pat_"));
        String createdBody = created.asString();
        assertFalse(createdBody.contains("tokenHash"));

        given().header("Authorization", "Bearer " + plaintext)
                .when().get("/mcp/__whoami")
                .then().statusCode(200)
                .body("userName", equalTo("admin"));

        SysUserAccessToken row = tokenMapper.selectByHash(AccessTokens.sha256Hex(plaintext));
        assertNotNull(row);
        assertEquals(AccessTokens.displayPrefix(plaintext), row.getTokenPrefix());
        assertFalse(plaintext.equals(row.getTokenHash()));
        assertNotNull(row.getLastUsedAt());
        assertNotNull(row.getLastUsedIp());

        List<Map<String, Object>> list = given().header("Authorization", admin)
                .when().get("/system/user/profile/tokens")
                .then().statusCode(200).body("code", equalTo(200))
                .body("mcpUrl", startsWith("http"))
                .body("mcpUrl", containsString("/mcp"))
                .body(not(containsString(plaintext)))
                .extract().path("data");
        Map<String, Object> item = list.stream()
                .filter(m -> ((Number) m.get("tokenId")).longValue() == tokenId.longValue())
                .findFirst().orElseThrow();
        assertEquals("Cursor 测试", item.get("name"));
        assertNotNull(item.get("lastUsedAt"));
        assertFalse(item.containsKey("tokenHash"));

        given().header("Authorization", admin)
                .when().delete("/system/user/profile/tokens/" + tokenId)
                .then().statusCode(200).body("code", equalTo(200));

        given().header("Authorization", "Bearer " + plaintext)
                .when().get("/mcp/__whoami")
                .then().statusCode(401)
                .header("WWW-Authenticate", startsWith("Bearer"));
    }

    @Test
    void tokenOnlyWorksOnMcp() {
        String admin = QuarkusTestAuth.loginAsAdmin();
        String plaintext = createToken(admin, "仅 MCP", null).path("data.token");

        given().header("Authorization", "Bearer " + plaintext)
                .when().get("/system/defect/list")
                .then().statusCode(200)
                .body("code", equalTo(401));

        given().when().get("/mcp/__whoami").then().statusCode(401);
        given().header("Authorization", "Bearer " + AccessTokens.generate(AccessTokens.PERSONAL_PREFIX))
                .when().get("/mcp/__whoami").then().statusCode(401);
        given().header("Authorization", admin)
                .when().get("/mcp/__whoami").then().statusCode(401);
    }

    @Test
    void expiredTokenRejected() {
        String plaintext = AccessTokens.generate(AccessTokens.PERSONAL_PREFIX);
        Date now = new Date();
        tokenMapper.insert(tokenService.newRow(1L, "已过期", plaintext, SysUserAccessToken.SOURCE_MANUAL,
                new Date(now.getTime() - 1000L), now));
        given().header("Authorization", "Bearer " + plaintext)
                .when().get("/mcp/__whoami").then().statusCode(401);
    }

    @Test
    void createValidation() {
        String admin = QuarkusTestAuth.loginAsAdmin();
        given().header("Authorization", admin).contentType(ContentType.JSON)
                .body("{\"name\":\"\"}")
                .when().post("/system/user/profile/tokens")
                .then().body("code", not(equalTo(200)));
        given().header("Authorization", admin).contentType(ContentType.JSON)
                .body("{\"name\":\"x\",\"expireDays\":0}")
                .when().post("/system/user/profile/tokens")
                .then().body("code", not(equalTo(200)));
    }

    @Test
    void disabledOrDeletedUserTokenRejectedAndIsolated() {
        String admin = QuarkusTestAuth.loginAsAdmin();
        String suffix = Long.toString(System.nanoTime(), 36);
        String userName = "pat_" + suffix;
        long userId = createUser(admin, userName);
        try {
            String userJwt = login(userName);
            Response created = createToken(userJwt, "我的令牌", 30);
            String plaintext = created.path("data.token");
            Number tokenId = created.path("data.info.tokenId");

            given().header("Authorization", "Bearer " + plaintext)
                    .when().get("/mcp/__whoami")
                    .then().statusCode(200).body("userName", equalTo(userName));

            Number adminTokenId = createToken(admin, "管理员令牌", 30).path("data.info.tokenId");
            given().header("Authorization", userJwt)
                    .when().get("/system/user/profile/tokens")
                    .then().statusCode(200)
                    .body(not(containsString("管理员令牌")));
            given().header("Authorization", userJwt)
                    .when().delete("/system/user/profile/tokens/" + adminTokenId)
                    .then().statusCode(200).body("code", not(equalTo(200)));

            changeStatus(admin, userId, "1");
            given().header("Authorization", "Bearer " + plaintext)
                    .when().get("/mcp/__whoami").then().statusCode(401);

            changeStatus(admin, userId, "0");
            given().header("Authorization", "Bearer " + plaintext)
                    .when().get("/mcp/__whoami").then().statusCode(200);

            given().header("Authorization", admin)
                    .when().delete("/system/user/" + userId)
                    .then().statusCode(200).body("code", equalTo(200));
            userId = -1;
            given().header("Authorization", "Bearer " + plaintext)
                    .when().get("/mcp/__whoami").then().statusCode(401);
            assertNotNull(tokenId);
        } finally {
            if (userId > 0) {
                given().header("Authorization", admin).when().delete("/system/user/" + userId);
            }
        }
    }

    private static Response createToken(String jwt, String name, Integer expireDays) {
        String days = expireDays == null ? "null" : expireDays.toString();
        return given().header("Authorization", jwt)
                .contentType(ContentType.JSON)
                .body("{\"name\":\"" + name + "\",\"expireDays\":" + days + "}")
                .when().post("/system/user/profile/tokens")
                .then().statusCode(200).body("code", equalTo(200))
                .body("data.token", notNullValue())
                .extract().response();
    }

    private static void changeStatus(String admin, long userId, String status) {
        given().header("Authorization", admin)
                .contentType(ContentType.JSON)
                .body("{\"userId\":" + userId + ",\"status\":\"" + status + "\"}")
                .when().put("/system/user/changeStatus")
                .then().statusCode(200).body("code", equalTo(200));
    }

    private static long createUser(String admin, String userName) {
        given().header("Authorization", admin)
                .contentType(ContentType.JSON)
                .body("{\"userName\":\"" + userName + "\",\"nickName\":\"" + userName + "\",\"password\":\""
                        + PASSWORD + "\",\"status\":\"0\"}")
                .when().post("/system/user")
                .then().statusCode(200).body("code", equalTo(200));
        return given().header("Authorization", admin)
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
