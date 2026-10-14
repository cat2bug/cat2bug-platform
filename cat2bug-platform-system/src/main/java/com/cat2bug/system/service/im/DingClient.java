package com.cat2bug.system.service.im;

import com.cat2bug.common.domain.im.DingProjectConfig;
import com.cat2bug.common.util.StringUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.enterprise.context.ApplicationScoped;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 钉钉：群机器人 Webhook（可选加签）与企业内部机器人单聊消息。
 */
@ApplicationScoped
public class DingClient extends AbstractImClient {

    public static final String PLATFORM = "ding";

    /** 旧版 oapi 的 token 无效 / 过期错误码。 */
    private static final Set<Integer> OAPI_TOKEN_INVALID_CODES = Set.of(40014, 42001, 88);
    private static final String API_TOKEN_INVALID_CODE = "InvalidAuthentication";

    String apiBaseUrl = "https://api.dingtalk.com";
    String oapiBaseUrl = "https://oapi.dingtalk.com";

    public void sendGroupText(String hook, String secret, String content) throws Exception {
        ObjectNode body = MAPPER.createObjectNode();
        body.put("msgtype", "text");
        body.putObject("text").put("content", content);
        String url = StringUtils.isNotBlank(secret) ? signHookUrl(hook, secret, System.currentTimeMillis()) : hook;
        HttpResult result = postJson(url, null, body);
        JsonNode json = result.json();
        if (!result.successful() || json.path("errcode").asInt(-1) != 0) {
            throw new ImPushException("钉钉群机器人发送失败: HTTP " + result.status() + " " + abbreviate(result.body()));
        }
    }

    /** 企业内部机器人「卡片」消息：标题 + Markdown 正文 + 单个跳转按钮。 */
    public void sendRobotActionCard(DingProjectConfig app, String mobile, String title, String markdown,
                                    String buttonTitle, String buttonUrl) throws Exception {
        ObjectNode params = MAPPER.createObjectNode();
        params.put("title", title);
        params.put("text", markdown);
        params.put("singleTitle", buttonTitle);
        params.put("singleURL", buttonUrl);
        sendRobotMessage(app, mobile, "sampleActionCard", params);
    }

    public void sendRobotMessage(DingProjectConfig app, String mobile, String msgKey, ObjectNode msgParam)
            throws Exception {
        requireApp(app);
        withToken(PLATFORM, app.getAppKey(), () -> fetchToken(app), token -> {
            String userId = getUserIdByMobile(token, mobile);
            ObjectNode body = MAPPER.createObjectNode();
            body.put("robotCode", app.getRobotCode());
            body.put("msgKey", msgKey);
            body.put("msgParam", MAPPER.writeValueAsString(msgParam));
            body.putPOJO("userIds", List.of(userId));
            HttpResult result = postJson(stripTrailingSlash(apiBaseUrl) + "/v1.0/robot/oToMessages/batchSend",
                    Map.of("x-acs-dingtalk-access-token", token), body);
            if (!result.successful()) {
                if (API_TOKEN_INVALID_CODE.equals(result.json().path("code").asText())) {
                    throw new ImPushException.TokenInvalid("钉钉 token 无效");
                }
                throw new ImPushException("钉钉机器人单聊发送失败: HTTP " + result.status() + " " + abbreviate(result.body()));
            }
            return null;
        });
    }

    private String getUserIdByMobile(String token, String mobile) throws Exception {
        ObjectNode body = MAPPER.createObjectNode();
        body.put("mobile", mobile);
        HttpResult result = postJson(stripTrailingSlash(oapiBaseUrl) + "/topapi/v2/user/getbymobile?access_token=" + token,
                null, body);
        JsonNode json = result.json();
        int errcode = json.path("errcode").asInt(-1);
        if (errcode == 0) {
            String userId = json.path("result").path("userid").asText("");
            if (StringUtils.isBlank(userId)) {
                throw new ImPushException("钉钉未找到手机号对应用户: " + maskMobile(mobile));
            }
            return userId;
        }
        if (OAPI_TOKEN_INVALID_CODES.contains(errcode)) {
            throw new ImPushException.TokenInvalid("钉钉 token 无效: errcode=" + errcode);
        }
        throw new ImPushException("通过手机号获取钉钉用户ID失败(" + maskMobile(mobile) + "): errcode=" + errcode
                + " " + json.path("errmsg").asText(""));
    }

    private ImTokenCache.ImToken fetchToken(DingProjectConfig app) throws Exception {
        ObjectNode body = MAPPER.createObjectNode();
        body.put("appKey", app.getAppKey());
        body.put("appSecret", app.getAppSecret());
        HttpResult result = postJson(stripTrailingSlash(apiBaseUrl) + "/v1.0/oauth2/accessToken", null, body);
        JsonNode json = result.json();
        String token = json.path("accessToken").asText("");
        if (StringUtils.isBlank(token)) {
            throw new ImPushException("获取钉钉 token 失败: HTTP " + result.status() + " "
                    + json.path("code").asText("") + " " + json.path("message").asText(""));
        }
        return new ImTokenCache.ImToken(token, json.path("expireIn").asLong(0));
    }

    private static void requireApp(DingProjectConfig app) throws ImPushException {
        if (app == null || StringUtils.isBlank(app.getAppKey()) || StringUtils.isBlank(app.getAppSecret())
                || StringUtils.isBlank(app.getRobotCode())) {
            throw new ImPushException("钉钉企业应用配置不完整（appKey / appSecret / robotCode）");
        }
    }

    public static String signHookUrl(String hook, String secret, long timestampMillis) throws Exception {
        String separator = hook.contains("?") ? "&" : "?";
        return hook + separator + "timestamp=" + timestampMillis + "&sign=" + sign(timestampMillis, secret);
    }

    static String sign(long timestampMillis, String secret) throws Exception {
        String stringToSign = timestampMillis + "\n" + secret;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] signData = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
        return URLEncoder.encode(Base64.getEncoder().encodeToString(signData), StandardCharsets.UTF_8);
    }
}
