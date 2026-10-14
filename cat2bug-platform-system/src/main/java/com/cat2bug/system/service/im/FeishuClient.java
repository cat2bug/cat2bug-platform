package com.cat2bug.system.service.im;

import com.cat2bug.common.domain.im.FeishuProjectConfig;
import com.cat2bug.common.util.StringUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.enterprise.context.ApplicationScoped;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 飞书：群机器人 Webhook（可选签名校验）与企业自建应用单聊消息。
 */
@ApplicationScoped
public class FeishuClient extends AbstractImClient {

    public static final String PLATFORM = "feishu";

    private static final Set<Integer> TOKEN_INVALID_CODES = Set.of(99991661, 99991663, 99991664);

    String baseUrl = "https://open.feishu.cn";

    public void sendGroupText(String hook, String secret, String text) throws Exception {
        ObjectNode body = MAPPER.createObjectNode();
        if (StringUtils.isNotBlank(secret)) {
            long timestamp = System.currentTimeMillis() / 1000;
            body.put("timestamp", String.valueOf(timestamp));
            body.put("sign", sign(secret, timestamp));
        }
        body.put("msg_type", "text");
        body.putObject("content").put("text", text);
        HttpResult result = postJson(hook, null, body);
        JsonNode json = result.json();
        int code = json.has("code") ? json.path("code").asInt(-1) : json.path("StatusCode").asInt(-1);
        if (!result.successful() || code != 0) {
            throw new ImPushException("飞书群机器人发送失败: HTTP " + result.status() + " " + abbreviate(result.body()));
        }
    }

    public void sendUserText(FeishuProjectConfig app, String mobile, String text) throws Exception {
        if (app == null || StringUtils.isBlank(app.getAppId()) || StringUtils.isBlank(app.getAppSecret())) {
            throw new ImPushException("飞书企业应用 appId / appSecret 未配置");
        }
        withToken(PLATFORM, app.getAppId(), () -> fetchToken(app), token -> {
            String userId = lookupUserId(token, mobile);
            ObjectNode content = MAPPER.createObjectNode();
            content.put("text", text);
            ObjectNode body = MAPPER.createObjectNode();
            body.put("receive_id", userId);
            body.put("msg_type", "text");
            body.put("content", MAPPER.writeValueAsString(content));
            HttpResult result = postJson(stripTrailingSlash(baseUrl) + "/open-apis/im/v1/messages?receive_id_type=user_id",
                    Map.of("Authorization", "Bearer " + token), body);
            checkCode(result, "飞书单聊发送失败");
            return null;
        });
    }

    private String lookupUserId(String token, String mobile) throws Exception {
        ObjectNode body = MAPPER.createObjectNode();
        body.putPOJO("mobiles", List.of(mobile));
        HttpResult result = postJson(stripTrailingSlash(baseUrl) + "/open-apis/contact/v3/users/batch_get_id?user_id_type=user_id",
                Map.of("Authorization", "Bearer " + token), body);
        JsonNode json = checkCode(result, "飞书按手机号查询用户失败");
        JsonNode userList = json.path("data").path("user_list");
        String userId = userList.isArray() && !userList.isEmpty() ? userList.get(0).path("user_id").asText("") : "";
        if (StringUtils.isBlank(userId)) {
            throw new ImPushException("飞书未找到手机号对应用户: " + maskMobile(mobile));
        }
        return userId;
    }

    private JsonNode checkCode(HttpResult result, String action) throws ImPushException {
        JsonNode json = result.json();
        int code = json.path("code").asInt(-1);
        if (code == 0) {
            return json;
        }
        if (TOKEN_INVALID_CODES.contains(code)) {
            throw new ImPushException.TokenInvalid("飞书 token 无效: code=" + code);
        }
        throw new ImPushException(action + ": HTTP " + result.status() + " code=" + code + " "
                + json.path("msg").asText(""));
    }

    private ImTokenCache.ImToken fetchToken(FeishuProjectConfig app) throws Exception {
        ObjectNode body = MAPPER.createObjectNode();
        body.put("app_id", app.getAppId());
        body.put("app_secret", app.getAppSecret());
        HttpResult result = postJson(stripTrailingSlash(baseUrl) + "/open-apis/auth/v3/tenant_access_token/internal",
                null, body);
        JsonNode json = result.json();
        String token = json.path("tenant_access_token").asText("");
        if (json.path("code").asInt(-1) != 0 || StringUtils.isBlank(token)) {
            throw new ImPushException("获取飞书 tenant_access_token 失败: code=" + json.path("code").asInt(-1)
                    + " " + json.path("msg").asText(""));
        }
        return new ImTokenCache.ImToken(token, json.path("expire").asLong(0));
    }

    static String sign(String secret, long timestampSeconds) throws Exception {
        String stringToSign = timestampSeconds + "\n" + secret;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(stringToSign.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return Base64.getEncoder().encodeToString(mac.doFinal(new byte[0]));
    }
}
