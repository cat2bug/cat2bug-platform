package com.cat2bug.system.service.im;

import com.cat2bug.common.domain.im.EnterpriseWeChatProjectConfig;
import com.cat2bug.common.util.StringUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.enterprise.context.ApplicationScoped;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

/**
 * 企业微信：群机器人 Webhook 与自建应用消息。
 */
@ApplicationScoped
public class WeComClient extends AbstractImClient {

    public static final String PLATFORM = "wechat";

    private static final Set<Integer> TOKEN_INVALID_CODES = Set.of(40014, 41001, 42001);
    private static final int IP_NOT_ALLOWED_CODE = 60020;

    String baseUrl = "https://qyapi.weixin.qq.com";

    public void sendGroupMarkdown(String hook, String content, String mentionMobile) throws Exception {
        ObjectNode body = MAPPER.createObjectNode();
        body.put("msgtype", "markdown");
        body.putObject("markdown").put("content", content);
        if (StringUtils.isNotBlank(mentionMobile)) {
            body.putPOJO("mentioned_mobile_list", List.of(mentionMobile));
        }
        HttpResult result = postJson(hook, null, body);
        if (!result.successful() || result.json().path("errcode").asInt(-1) != 0) {
            throw new ImPushException("企业微信群机器人发送失败: HTTP " + result.status() + " " + abbreviate(result.body()));
        }
    }

    /**
     * 应用消息。{@code msgType} 为 {@code text} 或 {@code markdown}，正文放在同名字段的 {@code content} 里。
     */
    public void sendAppMessage(EnterpriseWeChatProjectConfig app, String mobile, String msgType, String content)
            throws Exception {
        if (app == null || StringUtils.isBlank(app.getCorpId()) || StringUtils.isBlank(app.getCorpSecret())
                || StringUtils.isBlank(app.getAgentid())) {
            throw new ImPushException("企业微信应用配置不完整（corpId / corpSecret / agentid）");
        }
        int agentId;
        try {
            agentId = Integer.parseInt(app.getAgentid().trim());
        } catch (NumberFormatException e) {
            throw new ImPushException("企业微信 agentid 不是数字");
        }
        String appKey = app.getCorpId() + ":" + agentId;
        withToken(PLATFORM, appKey, () -> fetchToken(app), token -> {
            String userId = getUserIdByMobile(token, mobile);
            ObjectNode body = MAPPER.createObjectNode();
            body.put("touser", userId);
            body.put("msgtype", msgType);
            body.put("agentid", agentId);
            body.putObject(msgType).put("content", content);
            HttpResult result = postJson(stripTrailingSlash(baseUrl) + "/cgi-bin/message/send?access_token=" + token,
                    null, body);
            checkErrcode(result, "企业微信应用消息发送失败");
            return null;
        });
    }

    private String getUserIdByMobile(String token, String mobile) throws Exception {
        ObjectNode body = MAPPER.createObjectNode();
        body.put("mobile", mobile);
        HttpResult result = postJson(stripTrailingSlash(baseUrl) + "/cgi-bin/user/getuserid?access_token=" + token,
                null, body);
        JsonNode json = checkErrcode(result, "通过手机号获取企业微信 userid 失败(" + maskMobile(mobile) + ")");
        String userId = json.path("userid").asText("");
        if (StringUtils.isBlank(userId)) {
            throw new ImPushException("企业微信未找到手机号对应用户: " + maskMobile(mobile));
        }
        return userId;
    }

    private JsonNode checkErrcode(HttpResult result, String action) throws ImPushException {
        JsonNode json = result.json();
        int errcode = json.path("errcode").asInt(-1);
        if (errcode == 0) {
            return json;
        }
        if (TOKEN_INVALID_CODES.contains(errcode)) {
            throw new ImPushException.TokenInvalid("企业微信 token 无效: errcode=" + errcode);
        }
        if (errcode == IP_NOT_ALLOWED_CODE) {
            throw new ImPushException("企业微信接口拒绝当前服务器IP访问，请在企业微信应用配置中添加可信IP");
        }
        throw new ImPushException(action + ": errcode=" + errcode + " " + json.path("errmsg").asText(""));
    }

    private ImTokenCache.ImToken fetchToken(EnterpriseWeChatProjectConfig app) throws Exception {
        String url = stripTrailingSlash(baseUrl) + "/cgi-bin/gettoken?corpid="
                + URLEncoder.encode(app.getCorpId(), StandardCharsets.UTF_8)
                + "&corpsecret=" + URLEncoder.encode(app.getCorpSecret(), StandardCharsets.UTF_8);
        HttpResult result = get(url);
        JsonNode json = result.json();
        String token = json.path("access_token").asText("");
        if (json.path("errcode").asInt(-1) != 0 || StringUtils.isBlank(token)) {
            throw new ImPushException("获取企业微信 token 失败: errcode=" + json.path("errcode").asInt(-1)
                    + " " + json.path("errmsg").asText(""));
        }
        return new ImTokenCache.ImToken(token, json.path("expires_in").asLong(0));
    }
}
