package com.cat2bug.admin;

import com.cat2bug.common.domain.SysOAuthCode;
import com.cat2bug.common.security.AccessTokens;
import com.cat2bug.system.mapper.SysOAuthCodeMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class QuarkusOAuthTest {

    private static final String REDIRECT = "http://127.0.0.1:33418/callback";
    private static final AtomicInteger IP_SEQ = new AtomicInteger(1);

    @Inject
    SysOAuthCodeMapper codeMapper;

    /** 每个用例换一个来源地址，避免进程内限流计数互相影响 */
    private static RequestSpecification client() {
        return given().header("X-Forwarded-For", "203.0.113." + IP_SEQ.getAndIncrement());
    }

    @Test
    void metadataDerivedFromRequest() {
        given().when().get("/.well-known/oauth-authorization-server")
                .then().statusCode(200)
                .body("issuer", equalTo("http://localhost:8081"))
                .body("authorization_endpoint", equalTo("http://localhost:8081/oauth/authorize"))
                .body("token_endpoint", equalTo("http://localhost:8081/oauth/token"))
                .body("registration_endpoint", equalTo("http://localhost:8081/oauth/register"))
                .body("code_challenge_methods_supported", hasItem("S256"))
                .body("token_endpoint_auth_methods_supported", hasItem("none"));

        given().header("X-Forwarded-Proto", "https").header("X-Forwarded-Host", "bug.example.com")
                .accept("text/html,application/json")
                .when().get("/.well-known/oauth-authorization-server")
                .then().statusCode(200)
                .contentType(ContentType.JSON)
                .body("issuer", equalTo("https://bug.example.com"));
    }

    @Test
    void registrationValidation() {
        String clientId = register("Claude", List.of("https://claude.ai/api/mcp/auth_callback"));
        assertTrue(clientId.startsWith("c2b_client_"));

        client().contentType(ContentType.JSON)
                .body("{\"client_name\":\"x\",\"redirect_uris\":[\"http://evil.example.com/cb\"]}")
                .when().post("/oauth/register")
                .then().statusCode(400).body("error", notNullValue());
        client().contentType(ContentType.JSON)
                .body("{\"client_name\":\"x\",\"redirect_uris\":[]}")
                .when().post("/oauth/register")
                .then().statusCode(400);
        client().contentType(ContentType.JSON)
                .body("{\"client_name\":\"x\",\"redirect_uris\":[\"" + REDIRECT + "\"],\"token_endpoint_auth_method\":\"client_secret_basic\"}")
                .when().post("/oauth/register")
                .then().statusCode(400).body("error", equalTo("invalid_client_metadata"));
    }

    @Test
    void registerRateLimited() {
        String ip = "198.51.100.77";
        int created = 0;
        int limited = 0;
        for (int i = 0; i < 12; i++) {
            int status = given().header("X-Forwarded-For", ip).contentType(ContentType.JSON)
                    .body("{\"client_name\":\"flood\",\"redirect_uris\":[\"" + REDIRECT + "\"]}")
                    .when().post("/oauth/register").statusCode();
            if (status == 201) {
                created++;
            } else if (status == 429) {
                limited++;
            }
        }
        assertEquals(10, created);
        assertEquals(2, limited);
        given().header("X-Forwarded-For", "198.51.100.78").contentType(ContentType.JSON)
                .body("{\"client_name\":\"other\",\"redirect_uris\":[\"" + REDIRECT + "\"]}")
                .when().post("/oauth/register").then().statusCode(201);
    }

    @Test
    void authorizeValidation() {
        String clientId = register("Inspector", List.of(REDIRECT));

        given().redirects().follow(false).accept("text/html")
                .queryParam("response_type", "code").queryParam("client_id", "c2b_client_unknown")
                .queryParam("redirect_uri", REDIRECT).queryParam("code_challenge", "x")
                .queryParam("code_challenge_method", "S256")
                .when().get("/oauth/authorize")
                .then().statusCode(400).contentType(containsString("text/html"));

        given().redirects().follow(false)
                .queryParam("response_type", "code").queryParam("client_id", clientId)
                .queryParam("redirect_uri", "http://127.0.0.1:9999/other")
                .queryParam("code_challenge", "x").queryParam("code_challenge_method", "S256")
                .when().get("/oauth/authorize")
                .then().statusCode(400);

        String noPkce = given().redirects().follow(false)
                .queryParam("response_type", "code").queryParam("client_id", clientId)
                .queryParam("redirect_uri", REDIRECT).queryParam("state", "s1")
                .when().get("/oauth/authorize")
                .then().statusCode(302).extract().header("Location");
        assertTrue(noPkce.startsWith(REDIRECT + "?error=invalid_request"), noPkce);
        assertTrue(noPkce.contains("state=s1"));

        String consent = given().redirects().follow(false).accept("text/html")
                .queryParam("response_type", "code").queryParam("client_id", clientId)
                .queryParam("redirect_uri", REDIRECT).queryParam("state", "s2")
                .queryParam("code_challenge", AccessTokens.pkceChallenge(verifier()))
                .queryParam("code_challenge_method", "S256")
                .when().get("/oauth/authorize")
                .then().statusCode(302).extract().header("Location");
        assertTrue(consent.contains("/oauth/consent?"), consent);
        assertTrue(consent.contains("client_id=" + clientId));

        String admin = QuarkusTestAuth.loginAsAdmin();
        given().header("Authorization", admin).contentType(ContentType.JSON)
                .body(authorizeBody(clientId, AccessTokens.pkceChallenge(verifier()), "s3", null))
                .when().post("/oauth/authorize/check")
                .then().statusCode(200).body("code", equalTo(200))
                .body("data.clientName", equalTo("Inspector"))
                .body("data.redirectHost", equalTo("127.0.0.1"));
        given().contentType(ContentType.JSON)
                .body(authorizeBody(clientId, "x", "s3", true))
                .when().post("/oauth/authorize/decision")
                .then().statusCode(200).body("code", equalTo(401));

        Response denied = given().header("Authorization", admin).contentType(ContentType.JSON)
                .body(authorizeBody(clientId, "x", "s4", false))
                .when().post("/oauth/authorize/decision");
        String deniedRedirect = denied.path("data.redirect");
        assertTrue(deniedRedirect.startsWith(REDIRECT + "?error=access_denied"), deniedRedirect);
        assertTrue(deniedRedirect.contains("state=s4"));
    }

    @Test
    void fullFlowWithRefreshRotationAndReplay() {
        String admin = QuarkusTestAuth.loginAsAdmin();
        String clientId = register("Claude Desktop", List.of(REDIRECT));
        String verifier = verifier();
        String code = approve(admin, clientId, verifier, "st");

        client().contentType(ContentType.URLENC)
                .formParam("grant_type", "authorization_code").formParam("code", code)
                .formParam("client_id", clientId).formParam("redirect_uri", REDIRECT)
                .formParam("code_verifier", verifier())
                .when().post("/oauth/token")
                .then().statusCode(400).body("error", equalTo("invalid_grant"));
        client().contentType(ContentType.URLENC)
                .formParam("grant_type", "authorization_code").formParam("code", code)
                .formParam("client_id", clientId).formParam("redirect_uri", REDIRECT)
                .formParam("code_verifier", verifier)
                .when().post("/oauth/token")
                .then().statusCode(400).body("error", equalTo("invalid_grant"));

        code = approve(admin, clientId, verifier, "st2");
        Response tokens = client().contentType(ContentType.URLENC)
                .formParam("grant_type", "authorization_code").formParam("code", code)
                .formParam("client_id", clientId).formParam("redirect_uri", REDIRECT)
                .formParam("code_verifier", verifier)
                .when().post("/oauth/token");
        tokens.then().statusCode(200)
                .header("Cache-Control", "no-store")
                .body("token_type", equalTo("Bearer"))
                .body("expires_in", equalTo(3600))
                .body("access_token", startsWith("c2b_oat_"))
                .body("refresh_token", startsWith("c2b_ort_"));
        String access = tokens.path("access_token");
        String refresh = tokens.path("refresh_token");

        client().contentType(ContentType.URLENC)
                .formParam("grant_type", "authorization_code").formParam("code", code)
                .formParam("client_id", clientId).formParam("code_verifier", verifier)
                .when().post("/oauth/token")
                .then().statusCode(400).body("error", equalTo("invalid_grant"));

        given().header("Authorization", "Bearer " + access)
                .when().get("/mcp/__whoami").then().statusCode(200).body("userName", equalTo("admin"));
        given().header("Authorization", "Bearer " + refresh)
                .when().get("/mcp/__whoami").then().statusCode(401);
        given().header("Authorization", "Bearer " + access)
                .when().get("/system/defect/list").then().body("code", equalTo(401));

        Response rotated = refresh(clientId, refresh);
        rotated.then().statusCode(200);
        String access2 = rotated.path("access_token");
        String refresh2 = rotated.path("refresh_token");
        given().header("Authorization", "Bearer " + access2)
                .when().get("/mcp/__whoami").then().statusCode(200);

        refresh(clientId, refresh).then().statusCode(400).body("error", equalTo("invalid_grant"));
        given().header("Authorization", "Bearer " + access2)
                .when().get("/mcp/__whoami").then().statusCode(401);
        refresh(clientId, refresh2).then().statusCode(400).body("error", equalTo("invalid_grant"));
    }

    @Test
    void revokeFromProfileAndEndpoint() {
        String admin = QuarkusTestAuth.loginAsAdmin();
        String clientName = "Revoke " + System.nanoTime();
        String clientId = register(clientName, List.of(REDIRECT));
        String verifier = verifier();
        Response tokens = exchange(clientId, approve(admin, clientId, verifier, null), verifier);
        String access = tokens.path("access_token");
        String refresh = tokens.path("refresh_token");

        List<Map<String, Object>> list = given().header("Authorization", admin)
                .when().get("/system/user/profile/tokens")
                .then().statusCode(200).extract().path("data");
        Map<String, Object> entry = list.stream()
                .filter(m -> clientName.equals(m.get("clientName")))
                .findFirst().orElseThrow();
        assertEquals("oauth_refresh", entry.get("source"));
        given().header("Authorization", admin)
                .when().delete("/system/user/profile/tokens/" + entry.get("tokenId"))
                .then().body("code", equalTo(200));
        given().header("Authorization", "Bearer " + access)
                .when().get("/mcp/__whoami").then().statusCode(401);
        refresh(clientId, refresh).then().statusCode(400);

        tokens = exchange(clientId, approve(admin, clientId, verifier, null), verifier);
        String access3 = tokens.path("access_token");
        String refresh3 = tokens.path("refresh_token");
        client().contentType(ContentType.URLENC).formParam("token", refresh3)
                .when().post("/oauth/revoke").then().statusCode(200);
        given().header("Authorization", "Bearer " + access3)
                .when().get("/mcp/__whoami").then().statusCode(401);
        client().contentType(ContentType.URLENC).formParam("token", "c2b_ort_unknown")
                .when().post("/oauth/revoke").then().statusCode(200);
    }

    @Test
    void expiredCodeRejected() {
        String clientId = register("Expired", List.of(REDIRECT));
        String verifier = verifier();
        String code = AccessTokens.generate(AccessTokens.OAUTH_CODE_PREFIX);
        Date now = new Date();
        SysOAuthCode row = new SysOAuthCode();
        row.setCodeHash(AccessTokens.sha256Hex(code));
        row.setClientId(clientId);
        row.setUserId(1L);
        row.setRedirectUri(REDIRECT);
        row.setCodeChallenge(AccessTokens.pkceChallenge(verifier));
        row.setExpiresAt(new Date(now.getTime() - 1000L));
        row.setCreateTime(now);
        codeMapper.insert(row);

        client().contentType(ContentType.URLENC)
                .formParam("grant_type", "authorization_code").formParam("code", code)
                .formParam("client_id", clientId).formParam("code_verifier", verifier)
                .when().post("/oauth/token")
                .then().statusCode(400).body("error", equalTo("invalid_grant"));
    }

    private static String register(String name, List<String> redirects) {
        StringBuilder uris = new StringBuilder();
        for (String uri : redirects) {
            uris.append(uris.length() == 0 ? "" : ",").append('"').append(uri).append('"');
        }
        return client().contentType(ContentType.JSON)
                .body("{\"client_name\":\"" + name + "\",\"redirect_uris\":[" + uris + "]}")
                .when().post("/oauth/register")
                .then().statusCode(201)
                .body("client_id", notNullValue())
                .extract().path("client_id");
    }

    private static String approve(String admin, String clientId, String verifier, String state) {
        String redirect = given().header("Authorization", admin).contentType(ContentType.JSON)
                .body(authorizeBody(clientId, AccessTokens.pkceChallenge(verifier), state, true))
                .when().post("/oauth/authorize/decision")
                .then().statusCode(200).body("code", equalTo(200))
                .extract().path("data.redirect");
        Map<String, String> query = query(redirect);
        if (state != null) {
            assertEquals(state, query.get("state"));
        }
        assertNotNull(query.get("code"));
        return query.get("code");
    }

    private static Response exchange(String clientId, String code, String verifier) {
        Response response = client().contentType(ContentType.URLENC)
                .formParam("grant_type", "authorization_code").formParam("code", code)
                .formParam("client_id", clientId).formParam("redirect_uri", REDIRECT)
                .formParam("code_verifier", verifier)
                .when().post("/oauth/token");
        response.then().statusCode(200);
        return response;
    }

    private static Response refresh(String clientId, String refresh) {
        return client().contentType(ContentType.URLENC)
                .formParam("grant_type", "refresh_token").formParam("refresh_token", refresh)
                .formParam("client_id", clientId)
                .when().post("/oauth/token");
    }

    private static String authorizeBody(String clientId, String challenge, String state, Boolean approve) {
        return "{\"response_type\":\"code\",\"client_id\":\"" + clientId + "\",\"redirect_uri\":\"" + REDIRECT
                + "\",\"code_challenge\":\"" + challenge + "\",\"code_challenge_method\":\"S256\""
                + (state == null ? "" : ",\"state\":\"" + state + "\"")
                + (approve == null ? "" : ",\"approve\":" + approve) + "}";
    }

    private static String verifier() {
        return AccessTokens.generate("v").substring(1) + "-._~";
    }

    private static Map<String, String> query(String url) {
        Map<String, String> result = new LinkedHashMap<>();
        String raw = URI.create(url).getRawQuery();
        if (raw == null) {
            return result;
        }
        for (String pair : raw.split("&")) {
            int idx = pair.indexOf('=');
            result.put(pair.substring(0, idx), URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8));
        }
        return result;
    }
}
