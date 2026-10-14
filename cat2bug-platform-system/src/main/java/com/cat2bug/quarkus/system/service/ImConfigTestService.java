package com.cat2bug.quarkus.system.service;

import com.cat2bug.quarkus.common.domain.im.*;
import com.cat2bug.quarkus.common.util.StringUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import okhttp3.*;
import org.jboss.logging.Logger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * IM 配置测试消息发送（仅用于配置页面的 group-test / single-test 接口）。
 */
@ApplicationScoped
public class ImConfigTestService {

    private static final Logger LOG = Logger.getLogger(ImConfigTestService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final MediaType JSON_TYPE = MediaType.parse("application/json;charset=utf-8");

    private static final String DING_GET_TOKEN_URL = "https://api.dingtalk.com/v1.0/oauth2/accessToken";
    private static final String DING_GET_USER_BY_MOBILE_URL = "https://oapi.dingtalk.com/topapi/v2/user/getbymobile";
    private static final String DING_SEND_INTERNAL_ROBOT_URL = "https://api.dingtalk.com/v1.0/robot/oToMessages/batchSend";

    private static final String WECHAT_GET_TOKEN_URL = "https://qyapi.weixin.qq.com/cgi-bin/gettoken?corpid=%s&corpsecret=%s";
    private static final String WECHAT_GET_USERID_URL = "https://qyapi.weixin.qq.com/cgi-bin/user/getuserid?access_token=%s";
    private static final String WECHAT_SEND_MESSAGE_URL = "https://qyapi.weixin.qq.com/cgi-bin/message/send?access_token=%s";

    private static final String FEISHU_GET_TOKEN_URL = "https://open.feishu.cn/open-apis/auth/v3/tenant_access_token/internal";
    private static final String FEISHU_LOOKUP_USER_URL = "https://open.feishu.cn/open-apis/contact/v3/users/batch_get_id?user_id_type=user_id";
    private static final String FEISHU_SEND_MESSAGE_URL = "https://open.feishu.cn/open-apis/im/v1/messages?receive_id_type=user_id";

    private final Map<String, String> tokenCache = new ConcurrentHashMap<>();

    @Inject
    ImProjectConfigService imProjectConfigService;

    public void sendDingGroupTest(Long projectId, Long memberId, Map<String, String> config) throws Exception {
        String hook = config.get("hook");
        if (StringUtils.isBlank(hook)) {
            throw new IllegalArgumentException("请配置群发 Hook 地址");
        }
        String key = config.get("key");
        String secret = config.get("secret");
        String testMessage = "这是来自 Cat2Bug 的钉钉测试消息";
        if (StringUtils.isNotBlank(key)) {
            testMessage = testMessage + "，关键词：" + key;
        }

        ObjectNode body = MAPPER.createObjectNode();
        body.put("msgtype", "text");
        ObjectNode text = body.putObject("text");
        text.put("content", testMessage);

        OkHttpClient client = buildClient();
        postDingHook(client, hook, secret, MAPPER.writeValueAsString(body));
    }

    public void sendDingSingleTest(Long projectId, Long memberId, Map<String, String> config) throws Exception {
        String mobile = config.get("mobile");
        if (StringUtils.isBlank(mobile)) {
            throw new IllegalArgumentException("请配置单发用户手机号");
        }
        IMProjectConfig projectConfig = imProjectConfigService.selectImProjectConfigByProjectIdAndSystemCode(
                projectId, DingProjectConfig.SYSTEM_CODE);
        if (projectConfig == null || StringUtils.isBlank(projectConfig.getConfigParams())) {
            throw new IllegalArgumentException("请先在项目配置中设置钉钉企业应用配置");
        }
        DingProjectConfig dingProjectConfig = MAPPER.readValue(projectConfig.getConfigParams(), DingProjectConfig.class);

        OkHttpClient client = buildClient();
        String token = getDingToken(client, dingProjectConfig.getAppKey(), dingProjectConfig.getAppSecret());
        String userId = getDingUserIdByMobile(client, token, mobile);

        ObjectNode msgParams = MAPPER.createObjectNode();
        msgParams.put("title", "Cat2Bug 测试消息");
        msgParams.put("text", "这是来自 Cat2Bug 的钉钉测试消息");
        msgParams.put("singleTitle", "确认");
        msgParams.put("singleURL", "https://www.cat2bug.com");

        ObjectNode body = MAPPER.createObjectNode();
        body.put("robotCode", dingProjectConfig.getRobotCode());
        body.put("msgKey", "sampleMarkdown");
        body.put("msgParam", MAPPER.writeValueAsString(msgParams));
        body.putPOJO("userIds", List.of(userId));

        Request request = new Request.Builder()
                .header("x-acs-dingtalk-access-token", token)
                .url(DING_SEND_INTERNAL_ROBOT_URL)
                .post(RequestBody.create(MAPPER.writeValueAsString(body), JSON_TYPE))
                .build();
        execute(client, request);
    }

    public void sendWeChatSingleTest(Long projectId, Long memberId, Map<String, String> config) throws Exception {
        String mobile = config.get("mobile");
        if (StringUtils.isBlank(mobile)) {
            throw new IllegalArgumentException("请配置单发手机号");
        }
        IMProjectConfig projectConfig = imProjectConfigService.selectImProjectConfigByProjectIdAndSystemCode(
                projectId, EnterpriseWeChatProjectConfig.SYSTEM_CODE);
        if (projectConfig == null || StringUtils.isBlank(projectConfig.getConfigParams())) {
            throw new IllegalArgumentException("请先在项目配置中设置企业微信应用配置");
        }
        EnterpriseWeChatProjectConfig weChatConfig = MAPPER.readValue(
                projectConfig.getConfigParams(), EnterpriseWeChatProjectConfig.class);

        OkHttpClient client = buildClient();
        String tokenKey = projectId + "-" + weChatConfig.getAgentid();
        String token = tokenCache.computeIfAbsent(tokenKey, k -> {
            try {
                return getWeChatToken(client, weChatConfig.getCorpId(), weChatConfig.getCorpSecret());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        String userId = getWeChatUserIdByMobile(client, token, mobile);

        ObjectNode body = MAPPER.createObjectNode();
        body.put("touser", userId);
        body.put("msgtype", "text");
        body.put("agentid", Integer.parseInt(weChatConfig.getAgentid()));
        ObjectNode text = body.putObject("text");
        text.put("content", "这是来自 Cat2Bug 的企业微信测试消息");

        Request request = new Request.Builder()
                .url(String.format(WECHAT_SEND_MESSAGE_URL, token))
                .post(RequestBody.create(MAPPER.writeValueAsString(body), JSON_TYPE))
                .build();
        execute(client, request);
    }

    public void sendWeChatGroupTest(Long projectId, Long memberId, Map<String, String> config) throws Exception {
        String hook = config.get("hook");
        if (StringUtils.isBlank(hook)) {
            throw new IllegalArgumentException("请配置群发 Webhook 地址");
        }
        String mobile = config.get("mobile");

        ObjectNode body = MAPPER.createObjectNode();
        body.put("msgtype", "markdown");
        ObjectNode markdown = body.putObject("markdown");
        markdown.put("content", "这是来自 Cat2Bug 的企业微信群机器人测试消息");
        if (StringUtils.isNotBlank(mobile)) {
            body.putPOJO("mentioned_mobile_list", List.of(mobile));
        }

        OkHttpClient client = buildClient();
        Request request = new Request.Builder()
                .url(hook)
                .post(RequestBody.create(MAPPER.writeValueAsString(body), JSON_TYPE))
                .build();
        execute(client, request);
    }

    public void sendFeishuGroupTest(Long projectId, Long memberId, Map<String, String> config) throws Exception {
        String hook = config.get("hook");
        if (StringUtils.isBlank(hook)) {
            throw new IllegalArgumentException("请配置群发 Hook 地址");
        }
        String key = config.get("key");
        String secret = config.get("secret");
        String testMessage = "这是来自 Cat2Bug 的飞书测试消息";
        if (StringUtils.isNotBlank(key)) {
            testMessage = testMessage + "，关键词：" + key;
        }

        Map<String, Object> body = new LinkedHashMap<>();
        if (StringUtils.isNotBlank(secret)) {
            long timestamp = System.currentTimeMillis() / 1000;
            body.put("timestamp", String.valueOf(timestamp));
            body.put("sign", feishuSign(secret, timestamp));
        }
        body.put("msg_type", "text");
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("text", testMessage);
        body.put("content", content);

        OkHttpClient client = buildClient();
        Request request = new Request.Builder()
                .url(hook)
                .post(RequestBody.create(MAPPER.writeValueAsString(body), JSON_TYPE))
                .build();
        execute(client, request);
    }

    public void sendFeishuSingleTest(Long projectId, Long memberId, Map<String, String> config) throws Exception {
        String mobile = config.get("mobile");
        if (StringUtils.isBlank(mobile)) {
            throw new IllegalArgumentException("请配置单发用户手机号");
        }
        IMProjectConfig projectConfig = imProjectConfigService.selectImProjectConfigByProjectIdAndSystemCode(
                projectId, FeishuProjectConfig.SYSTEM_CODE);
        if (projectConfig == null || StringUtils.isBlank(projectConfig.getConfigParams())) {
            throw new IllegalArgumentException("请先在项目配置中设置飞书企业应用 appId 和 appSecret");
        }
        FeishuProjectConfig feishuProjectConfig = MAPPER.readValue(projectConfig.getConfigParams(), FeishuProjectConfig.class);
        if (StringUtils.isBlank(feishuProjectConfig.getAppId()) || StringUtils.isBlank(feishuProjectConfig.getAppSecret())) {
            throw new IllegalArgumentException("请先在项目配置中设置飞书企业应用 appId 和 appSecret");
        }

        OkHttpClient client = buildClient();
        String tokenKey = feishuProjectConfig.getAppId();
        String token = tokenCache.computeIfAbsent(tokenKey, k -> {
            try {
                return getFeishuTenantToken(client, feishuProjectConfig.getAppId(), feishuProjectConfig.getAppSecret());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        String userId = lookupFeishuUserId(client, token, mobile);

        ObjectNode content = MAPPER.createObjectNode();
        content.put("text", "这是来自 Cat2Bug 的飞书测试消息");
        ObjectNode body = MAPPER.createObjectNode();
        body.put("receive_id", userId);
        body.put("msg_type", "text");
        body.set("content", MAPPER.valueToTree(MAPPER.writeValueAsString(content)));

        Request request = new Request.Builder()
                .header("Authorization", "Bearer " + token)
                .url(FEISHU_SEND_MESSAGE_URL)
                .post(RequestBody.create(MAPPER.writeValueAsString(body), JSON_TYPE))
                .build();
        execute(client, request);
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> extractConfigMap(Map<String, Object> params) {
        Object configObj = params.get("config");
        if (configObj instanceof Map<?, ?> map) {
            Map<String, String> result = new LinkedHashMap<>();
            map.forEach((k, v) -> result.put(String.valueOf(k), v != null ? String.valueOf(v) : null));
            return result;
        }
        throw new IllegalArgumentException("config 参数无效");
    }

    private OkHttpClient buildClient() {
        return new OkHttpClient.Builder()
                .readTimeout(30, TimeUnit.SECONDS)
                .connectTimeout(10, TimeUnit.SECONDS)
                .build();
    }

    private void postDingHook(OkHttpClient client, String hook, String secret, String bodyJson) throws Exception {
        String url = hook;
        if (StringUtils.isNotBlank(secret)) {
            long timestamp = System.currentTimeMillis();
            String sign = dingSign(timestamp, secret);
            String separator = url.contains("?") ? "&" : "?";
            url = url + separator + "timestamp=" + timestamp + "&sign=" + sign;
        }
        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(bodyJson, JSON_TYPE))
                .build();
        execute(client, request);
    }

    private String getDingToken(OkHttpClient client, String appKey, String appSecret) throws Exception {
        ObjectNode params = MAPPER.createObjectNode();
        params.put("appKey", appKey);
        params.put("appSecret", appSecret);
        Request request = new Request.Builder()
                .url(DING_GET_TOKEN_URL)
                .post(RequestBody.create(MAPPER.writeValueAsString(params), JSON_TYPE))
                .build();
        String json = execute(client, request);
        JsonNode node = MAPPER.readTree(json);
        JsonNode token = node.get("accessToken");
        if (token == null || token.isNull()) {
            throw new Exception("获取钉钉 token 异常: " + json);
        }
        return token.asText();
    }

    private String getDingUserIdByMobile(OkHttpClient client, String token, String mobile) throws Exception {
        ObjectNode body = MAPPER.createObjectNode();
        body.put("mobile", mobile);
        Request request = new Request.Builder()
                .url(DING_GET_USER_BY_MOBILE_URL + "?access_token=" + token)
                .post(RequestBody.create(MAPPER.writeValueAsString(body), JSON_TYPE))
                .build();
        String json = execute(client, request);
        JsonNode node = MAPPER.readTree(json);
        if (node.path("errcode").asInt(-1) != 0) {
            throw new Exception("通过手机号获取钉钉用户ID失败: " + json);
        }
        return node.path("result").path("userid").asText();
    }

    private String getWeChatToken(OkHttpClient client, String corpId, String corpSecret) throws Exception {
        Request request = new Request.Builder()
                .url(String.format(WECHAT_GET_TOKEN_URL, corpId, corpSecret))
                .build();
        String json = execute(client, request);
        JsonNode node = MAPPER.readTree(json);
        if (node.path("errcode").asInt(-1) != 0) {
            throw new Exception("获取企业微信 token 异常: " + json);
        }
        return node.path("access_token").asText();
    }

    private String getWeChatUserIdByMobile(OkHttpClient client, String token, String mobile) throws Exception {
        ObjectNode body = MAPPER.createObjectNode();
        body.put("mobile", mobile);
        Request request = new Request.Builder()
                .url(String.format(WECHAT_GET_USERID_URL, token))
                .post(RequestBody.create(MAPPER.writeValueAsString(body), JSON_TYPE))
                .build();
        String json = execute(client, request);
        JsonNode node = MAPPER.readTree(json);
        int errcode = node.path("errcode").asInt(-1);
        if (errcode == 0) {
            return node.path("userid").asText();
        }
        if (errcode == 60020) {
            throw new Exception("企业微信接口拒绝当前服务器IP访问，请在企业微信应用配置中添加可信IP");
        }
        throw new Exception("通过手机号获取企业微信 userid 失败: " + json);
    }

    private String getFeishuTenantToken(OkHttpClient client, String appId, String appSecret) throws Exception {
        ObjectNode body = MAPPER.createObjectNode();
        body.put("app_id", appId);
        body.put("app_secret", appSecret);
        Request request = new Request.Builder()
                .url(FEISHU_GET_TOKEN_URL)
                .post(RequestBody.create(MAPPER.writeValueAsString(body), JSON_TYPE))
                .build();
        String json = execute(client, request);
        JsonNode node = MAPPER.readTree(json);
        if (node.path("code").asInt(-1) != 0) {
            throw new Exception("获取飞书 tenant_access_token 异常: " + json);
        }
        return node.path("tenant_access_token").asText();
    }

    private String lookupFeishuUserId(OkHttpClient client, String token, String mobile) throws Exception {
        ObjectNode body = MAPPER.createObjectNode();
        body.putPOJO("mobiles", List.of(mobile));
        Request request = new Request.Builder()
                .header("Authorization", "Bearer " + token)
                .url(FEISHU_LOOKUP_USER_URL)
                .post(RequestBody.create(MAPPER.writeValueAsString(body), JSON_TYPE))
                .build();
        String json = execute(client, request);
        JsonNode node = MAPPER.readTree(json);
        if (node.path("code").asInt(-1) != 0) {
            throw new Exception("飞书单发未找到对应 user_id: " + json);
        }
        JsonNode userList = node.path("data").path("user_list");
        if (!userList.isArray() || userList.isEmpty()) {
            throw new Exception("飞书单发未找到对应 user_id, mobile=" + mobile);
        }
        return userList.get(0).path("user_id").asText();
    }

    private String execute(OkHttpClient client, Request request) throws Exception {
        try (Response response = client.newCall(request).execute()) {
            String json = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                LOG.errorf("IM test request failed: %s %s", response.code(), json);
            }
            return json;
        }
    }

    private String dingSign(long timestamp, String secret) throws Exception {
        String stringToSign = timestamp + "\n" + secret;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] signData = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
        return URLEncoder.encode(Base64.getEncoder().encodeToString(signData), StandardCharsets.UTF_8);
    }

    private String feishuSign(String secret, long timestamp) throws Exception {
        String stringToSign = timestamp + "\n" + secret;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(stringToSign.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] signData = mac.doFinal(new byte[0]);
        return Base64.getEncoder().encodeToString(signData);
    }
}
