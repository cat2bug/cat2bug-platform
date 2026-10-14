package com.cat2bug.system.service.im;

import com.cat2bug.common.domain.im.DingProjectConfig;
import com.cat2bug.common.domain.im.EnterpriseWeChatProjectConfig;
import com.cat2bug.common.domain.im.FeishuProjectConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImClientsTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ImHttpStub stub;
    private ImTokenCache tokenCache;
    private AtomicLong now;

    @BeforeEach
    void setUp() throws Exception {
        stub = new ImHttpStub();
        tokenCache = new ImTokenCache();
        now = new AtomicLong(1_000_000L);
        tokenCache.setClock(now::get);
    }

    @AfterEach
    void tearDown() {
        stub.close();
    }

    @Test
    void dingGroupSignsUrlAndSendsText() throws Exception {
        stub.reply("/robot/send", 200, "{\"errcode\":0,\"errmsg\":\"ok\"}");
        DingClient client = ding();

        client.sendGroupText(stub.baseUrl() + "/robot/send?access_token=abc", "SEC123", "【cat2bug】hello");

        ImHttpStub.Recorded request = stub.requests("/robot/send").get(0);
        Map<String, String> query = parseQuery(request.query());
        assertEquals("abc", query.get("access_token"));
        long timestamp = Long.parseLong(query.get("timestamp"));
        assertEquals(hmacBase64("SEC123", timestamp + "\nSEC123"), query.get("sign"));
        JsonNode body = MAPPER.readTree(request.body());
        assertEquals("text", body.path("msgtype").asText());
        assertEquals("【cat2bug】hello", body.path("text").path("content").asText());
    }

    @Test
    void dingGroupErrcodeIsReportedAsFailure() {
        stub.reply("/robot/send", 200, "{\"errcode\":310000,\"errmsg\":\"keywords not in content\"}");
        assertThrows(ImPushException.class,
                () -> ding().sendGroupText(stub.baseUrl() + "/robot/send", null, "hello"));
    }

    @Test
    void dingRobotActionCardUsesCachedTokenAndRetriesOnInvalidAuthentication() throws Exception {
        stub.reply("/v1.0/oauth2/accessToken", 200, "{\"accessToken\":\"T1\",\"expireIn\":7200}")
                .reply("/v1.0/oauth2/accessToken", 200, "{\"accessToken\":\"T2\",\"expireIn\":7200}")
                .reply("/topapi/v2/user/getbymobile", 200, "{\"errcode\":0,\"result\":{\"userid\":\"U1\"}}")
                .reply("/v1.0/robot/oToMessages/batchSend", 401, "{\"code\":\"InvalidAuthentication\"}")
                .reply("/v1.0/robot/oToMessages/batchSend", 200, "{\"processQueryKey\":\"k\"}");
        DingClient client = ding();

        client.sendRobotActionCard(dingApp(), "13800000000", "标题", "## 正文", "点击查看详情",
                "http://h/team/proj/defect?projectId=1&defectId=2&screen=full");

        assertEquals(2, stub.requests("/v1.0/oauth2/accessToken").size());
        var sends = stub.requests("/v1.0/robot/oToMessages/batchSend");
        assertEquals(2, sends.size());
        assertEquals("T1", sends.get(0).header("x-acs-dingtalk-access-token"));
        assertEquals("T2", sends.get(1).header("x-acs-dingtalk-access-token"));
        JsonNode body = MAPPER.readTree(sends.get(1).body());
        assertEquals("ROBOT", body.path("robotCode").asText());
        assertEquals("sampleActionCard", body.path("msgKey").asText());
        assertEquals("U1", body.path("userIds").get(0).asText());
        JsonNode params = MAPPER.readTree(body.path("msgParam").asText());
        assertEquals("点击查看详情", params.path("singleTitle").asText());
        assertTrue(params.path("singleURL").asText().endsWith("&screen=full"));
    }

    @Test
    void feishuGroupBodyCarriesTimestampAndSign() throws Exception {
        stub.reply("/hook", 200, "{\"code\":0,\"msg\":\"success\"}");
        FeishuClient client = feishu();

        client.sendGroupText(stub.baseUrl() + "/hook", "FS", "【k】hello");

        JsonNode body = MAPPER.readTree(stub.requests("/hook").get(0).body());
        long timestamp = Long.parseLong(body.path("timestamp").asText());
        String expected = Base64.getEncoder().encodeToString(hmac(timestamp + "\nFS", new byte[0]));
        assertEquals(expected, body.path("sign").asText());
        assertEquals("text", body.path("msg_type").asText());
        assertEquals("【k】hello", body.path("content").path("text").asText());
    }

    @Test
    void feishuTokenInvalidRefreshesAndRetriesOnce() throws Exception {
        stub.reply("/open-apis/auth/v3/tenant_access_token/internal", 200,
                        "{\"code\":0,\"tenant_access_token\":\"F1\",\"expire\":7200}")
                .reply("/open-apis/auth/v3/tenant_access_token/internal", 200,
                        "{\"code\":0,\"tenant_access_token\":\"F2\",\"expire\":7200}")
                .reply("/open-apis/contact/v3/users/batch_get_id", 200,
                        "{\"code\":0,\"data\":{\"user_list\":[{\"user_id\":\"ou1\"}]}}")
                .reply("/open-apis/im/v1/messages", 400, "{\"code\":99991663,\"msg\":\"invalid token\"}")
                .reply("/open-apis/im/v1/messages", 200, "{\"code\":0}");

        feishu().sendUserText(feishuApp(), "13800000000", "hello");

        var sends = stub.requests("/open-apis/im/v1/messages");
        assertEquals(2, sends.size());
        assertEquals("Bearer F1", sends.get(0).header("Authorization"));
        assertEquals("Bearer F2", sends.get(1).header("Authorization"));
        JsonNode body = MAPPER.readTree(sends.get(1).body());
        assertEquals("ou1", body.path("receive_id").asText());
        assertEquals("hello", MAPPER.readTree(body.path("content").asText()).path("text").asText());
    }

    @Test
    void feishuRetryStillInvalidGivesUpAfterOneRetry() {
        stub.reply("/open-apis/auth/v3/tenant_access_token/internal", 200,
                        "{\"code\":0,\"tenant_access_token\":\"F1\",\"expire\":7200}")
                .reply("/open-apis/contact/v3/users/batch_get_id", 200,
                        "{\"code\":0,\"data\":{\"user_list\":[{\"user_id\":\"ou1\"}]}}")
                .reply("/open-apis/im/v1/messages", 400, "{\"code\":99991663,\"msg\":\"invalid token\"}");

        ImPushException ex = assertThrows(ImPushException.class,
                () -> feishu().sendUserText(feishuApp(), "13800000000", "hello"));

        assertEquals(2, stub.requests("/open-apis/im/v1/messages").size());
        assertFalse(ex.getMessage().contains("F1"));
    }

    @Test
    void weComTokenIsReusedUntilFiveMinutesBeforeExpiry() throws Exception {
        stub.reply("/cgi-bin/gettoken", 200, "{\"errcode\":0,\"access_token\":\"W1\",\"expires_in\":7200}")
                .reply("/cgi-bin/gettoken", 200, "{\"errcode\":0,\"access_token\":\"W2\",\"expires_in\":7200}")
                .reply("/cgi-bin/user/getuserid", 200, "{\"errcode\":0,\"userid\":\"zhangsan\"}")
                .reply("/cgi-bin/message/send", 200, "{\"errcode\":0}");
        WeComClient client = weCom();

        client.sendAppMessage(weComApp(), "13800000000", "markdown", "## a");
        now.addAndGet(60_000L);
        client.sendAppMessage(weComApp(), "13800000000", "markdown", "## b");
        assertEquals(1, stub.requests("/cgi-bin/gettoken").size());

        now.addAndGet((7200L - 5 * 60) * 1000L);
        client.sendAppMessage(weComApp(), "13800000000", "markdown", "## c");
        assertEquals(2, stub.requests("/cgi-bin/gettoken").size());

        var sends = stub.requests("/cgi-bin/message/send");
        assertEquals("access_token=W2", sends.get(2).query());
        JsonNode body = MAPPER.readTree(sends.get(2).body());
        assertEquals("zhangsan", body.path("touser").asText());
        assertEquals("markdown", body.path("msgtype").asText());
        assertEquals(1000002, body.path("agentid").asInt());
        assertEquals("## c", body.path("markdown").path("content").asText());
    }

    @Test
    void weComExpiredTokenErrcodeRefreshesAndRetries() throws Exception {
        stub.reply("/cgi-bin/gettoken", 200, "{\"errcode\":0,\"access_token\":\"W1\",\"expires_in\":7200}")
                .reply("/cgi-bin/gettoken", 200, "{\"errcode\":0,\"access_token\":\"W2\",\"expires_in\":7200}")
                .reply("/cgi-bin/user/getuserid", 200, "{\"errcode\":42001,\"errmsg\":\"access_token expired\"}")
                .reply("/cgi-bin/user/getuserid", 200, "{\"errcode\":0,\"userid\":\"zhangsan\"}")
                .reply("/cgi-bin/message/send", 200, "{\"errcode\":0}");

        weCom().sendAppMessage(weComApp(), "13800000000", "text", "hi");

        assertEquals(2, stub.requests("/cgi-bin/gettoken").size());
        assertEquals("access_token=W2", stub.requests("/cgi-bin/message/send").get(0).query());
    }

    @Test
    void weComGroupSendsMarkdownWithMention() throws Exception {
        stub.reply("/cgi-bin/webhook/send", 200, "{\"errcode\":0}");

        weCom().sendGroupMarkdown(stub.baseUrl() + "/cgi-bin/webhook/send?key=K", "## 缺陷", "13800000000");

        JsonNode body = MAPPER.readTree(stub.requests("/cgi-bin/webhook/send").get(0).body());
        assertEquals("markdown", body.path("msgtype").asText());
        assertEquals("## 缺陷", body.path("markdown").path("content").asText());
        assertEquals("13800000000", body.path("mentioned_mobile_list").get(0).asText());
    }

    @Test
    void maskMobileKeepsOnlyPrefixAndSuffix() {
        assertEquals("138****00", AbstractImClient.maskMobile("13800000000"));
        assertEquals("***", AbstractImClient.maskMobile("123"));
    }

    private DingClient ding() {
        DingClient client = new DingClient();
        client.tokenCache = tokenCache;
        client.apiBaseUrl = stub.baseUrl();
        client.oapiBaseUrl = stub.baseUrl();
        return client;
    }

    private FeishuClient feishu() {
        FeishuClient client = new FeishuClient();
        client.tokenCache = tokenCache;
        client.baseUrl = stub.baseUrl();
        return client;
    }

    private WeComClient weCom() {
        WeComClient client = new WeComClient();
        client.tokenCache = tokenCache;
        client.baseUrl = stub.baseUrl();
        return client;
    }

    private static DingProjectConfig dingApp() {
        DingProjectConfig app = new DingProjectConfig();
        app.setAppKey("AK");
        app.setAppSecret("AS");
        app.setRobotCode("ROBOT");
        return app;
    }

    private static FeishuProjectConfig feishuApp() {
        FeishuProjectConfig app = new FeishuProjectConfig();
        app.setAppId("cli_1");
        app.setAppSecret("sec");
        return app;
    }

    private static EnterpriseWeChatProjectConfig weComApp() {
        EnterpriseWeChatProjectConfig app = new EnterpriseWeChatProjectConfig();
        app.setCorpId("corp");
        app.setCorpSecret("csec");
        app.setAgentid("1000002");
        return app;
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> result = new HashMap<>();
        for (String pair : query.split("&")) {
            int idx = pair.indexOf('=');
            result.put(pair.substring(0, idx), URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8));
        }
        return result;
    }

    private static String hmacBase64(String key, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return Base64.getEncoder().encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }

    private static byte[] hmac(String key, byte[] data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return mac.doFinal(data);
    }
}
