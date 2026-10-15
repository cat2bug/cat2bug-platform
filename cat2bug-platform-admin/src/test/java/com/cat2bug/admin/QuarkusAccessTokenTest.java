package com.cat2bug.admin;

import com.cat2bug.common.domain.SysUserAccessToken;
import com.cat2bug.common.security.AccessTokens;
import com.cat2bug.system.mapper.SysUserAccessTokenMapper;
import com.cat2bug.system.service.SysUserAccessTokenService;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
class QuarkusAccessTokenTest {

    private static final String PASSWORD = "Cat2bug@123";
    private static final long PROJECT_ID = 9001L;

    @Inject
    SysUserAccessTokenMapper tokenMapper;

    @Inject
    SysUserAccessTokenService tokenService;

    @Test
    void oauthTokenCarriesUserAndProject() {
        String plaintext = issue(1L, PROJECT_ID, null);

        given().header("Authorization", "Bearer " + plaintext)
                .when().get("/mcp/__whoami")
                .then().statusCode(200)
                .body("userName", equalTo("admin"))
                .body("projectId", equalTo((int) PROJECT_ID));

        SysUserAccessToken row = tokenMapper.selectByHash(AccessTokens.sha256Hex(plaintext));
        assertNotNull(row);
        assertEquals(AccessTokens.displayPrefix(plaintext), row.getTokenPrefix());
        assertFalse(plaintext.equals(row.getTokenHash()));
        assertNotNull(row.getLastUsedAt());
        assertNotNull(row.getLastUsedIp());

        tokenMapper.revokeById(row.getTokenId(), new Date());
        given().header("Authorization", "Bearer " + plaintext)
                .when().get("/mcp/__whoami")
                .then().statusCode(401)
                .header("WWW-Authenticate", startsWith("Bearer"));
    }

    @Test
    void tokenOnlyWorksOnMcp() {
        String admin = QuarkusTestAuth.loginAsAdmin();
        String plaintext = issue(1L, PROJECT_ID, null);

        given().header("Authorization", "Bearer " + plaintext)
                .when().get("/system/defect/list")
                .then().statusCode(200)
                .body("code", equalTo(401));

        given().when().get("/mcp/__whoami").then().statusCode(401);
        given().header("Authorization", "Bearer " + AccessTokens.generate(AccessTokens.OAUTH_ACCESS_PREFIX))
                .when().get("/mcp/__whoami").then().statusCode(401);
        given().header("Authorization", admin)
                .when().get("/mcp/__whoami").then().statusCode(401);
    }

    @Test
    void personalTokensAreGone() {
        String admin = QuarkusTestAuth.loginAsAdmin();
        given().header("Authorization", admin)
                .when().get("/system/user/profile/tokens")
                .then().statusCode(404);
        given().header("Authorization", "Bearer c2b_pat_" + "a".repeat(43))
                .when().get("/mcp/__whoami").then().statusCode(401);
    }

    @Test
    void tokenWithoutProjectRejected() {
        String plaintext = issue(1L, null, null);
        given().header("Authorization", "Bearer " + plaintext)
                .when().get("/mcp/__whoami").then().statusCode(401);
    }

    @Test
    void expiredTokenRejected() {
        String plaintext = issue(1L, PROJECT_ID, new Date(System.currentTimeMillis() - 1000L));
        given().header("Authorization", "Bearer " + plaintext)
                .when().get("/mcp/__whoami").then().statusCode(401);
    }

    @Test
    void disabledOrDeletedUserTokenRejected() {
        String admin = QuarkusTestAuth.loginAsAdmin();
        String userName = "oat_" + Long.toString(System.nanoTime(), 36);
        long userId = createUser(admin, userName);
        try {
            String plaintext = issue(userId, PROJECT_ID, null);
            given().header("Authorization", "Bearer " + plaintext)
                    .when().get("/mcp/__whoami")
                    .then().statusCode(200).body("userName", equalTo(userName));

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
        } finally {
            if (userId > 0) {
                given().header("Authorization", admin).when().delete("/system/user/" + userId);
            }
        }
    }

    private String issue(long userId, Long projectId, Date expiresAt) {
        String plaintext = AccessTokens.generate(AccessTokens.OAUTH_ACCESS_PREFIX);
        Date now = new Date();
        Date expiry = expiresAt != null ? expiresAt : new Date(now.getTime() + 3_600_000L);
        tokenMapper.insert(tokenService.newRow(userId, projectId, "测试客户端", plaintext,
                SysUserAccessToken.SOURCE_OAUTH, expiry, now));
        return plaintext;
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
}
