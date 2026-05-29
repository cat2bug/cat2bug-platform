package com.cat2bug.im.service.impl;

import com.alibaba.fastjson.JSON;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.im.domain.*;
import com.cat2bug.im.service.IIMService;
import okhttp3.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class FeishuAppMessageServiceImpl implements IIMService<FeishuAppMessage, FeishuPlatformConfig> {
    public static final String MESSAGE_FACTORY_NAME = "ImFeishuAppMessage";
    private static final Logger log = LogManager.getLogger(FeishuAppMessageServiceImpl.class);
    private static final String GET_TOKEN_URL = "https://open.feishu.cn/open-apis/auth/v3/tenant_access_token/internal";
    private static final String LOOKUP_USER_URL = "https://open.feishu.cn/open-apis/contact/v3/users/batch_get_id?user_id_type=user_id";
    private static final String SEND_MESSAGE_URL = "https://open.feishu.cn/open-apis/im/v1/messages?receive_id_type=user_id";
    private static final MediaType JSON_CONTENT_TYPE = MediaType.parse("application/json;charset=utf-8");

    private final Map<String, String> tokenCache = new ConcurrentHashMap<>();

    @Override
    public String getMessageFactoryName() {
        return MESSAGE_FACTORY_NAME;
    }

    @Override
    public void sendNoticeMessage(FeishuAppMessage message, FeishuPlatformConfig config) throws Exception {
        if (config != null && !Boolean.TRUE.equals(config.getSingleSwitch())) {
            return;
        }
        if (config == null || StringUtils.isBlank(config.getMobile())) {
            log.warn("飞书单发手机号为空，无法发送信息 message:{}", JSON.toJSONString(message));
            return;
        }
        OkHttpClient client = new OkHttpClient().newBuilder().readTimeout(30, TimeUnit.SECONDS).connectTimeout(10, TimeUnit.SECONDS).build();
        String tokenCacheKey = getTokenCacheKey(message);
        String token = tokenCache.get(tokenCacheKey);
        if (StringUtils.isBlank(token)) {
            token = getTenantAccessToken(client, message);
            if (StringUtils.isBlank(token)) {
                return;
            }
            tokenCache.put(tokenCacheKey, token);
        }

        String userId = lookupUserIdByMobile(client, token, config.getMobile());
        if (StringUtils.isBlank(userId)) {
            log.warn("飞书单发未找到对应 user_id, mobile={}", config.getMobile());
            return;
        }
        message.setReceive_id(userId);
        sendMessage(client, token, message);
    }

    @Override
    public void receiveMessage(FeishuAppMessage message) {
    }

    private String getTenantAccessToken(OkHttpClient client, FeishuAppMessage message) throws IOException {
        String body = JSON.toJSONString(new TokenRequest(message.getAppId(), message.getAppSecret()));
        RequestBody requestBody = RequestBody.create(body, JSON_CONTENT_TYPE);
        Request request = new Request.Builder().url(GET_TOKEN_URL).post(requestBody).build();
        Response response = client.newCall(request).execute();
        String json = response.body() != null ? response.body().string() : "";
        FeishuTokenResult result = JSON.parseObject(json, FeishuTokenResult.class);
        if (result != null && result.getCode() == 0 && StringUtils.isNotBlank(result.getTenant_access_token())) {
            return result.getTenant_access_token();
        }
        log.warn("获取飞书 tenant_access_token 异常:{}", json);
        return null;
    }

    private String lookupUserIdByMobile(OkHttpClient client, String token, String mobile) throws IOException {
        String body = JSON.toJSONString(new LookupUserRequest(mobile));
        RequestBody requestBody = RequestBody.create(body, JSON_CONTENT_TYPE);
        Request request = new Request.Builder()
                .url(LOOKUP_USER_URL)
                .addHeader("Authorization", "Bearer " + token)
                .post(requestBody)
                .build();
        Response response = client.newCall(request).execute();
        String json = response.body() != null ? response.body().string() : "";
        FeishuUserLookupResult result = JSON.parseObject(json, FeishuUserLookupResult.class);
        if (result != null && result.getCode() == 0 && result.getData() != null && result.getData().getUser_list() != null && !result.getData().getUser_list().isEmpty()) {
            return result.getData().getUser_list().get(0).getUser_id();
        }
        log.warn("飞书用户查询失败 mobile={}, response={}", mobile, json);
        return null;
    }

    private void sendMessage(OkHttpClient client, String token, FeishuAppMessage message) throws IOException {
        String body = JSON.toJSONString(message);
        RequestBody requestBody = RequestBody.create(body, JSON_CONTENT_TYPE);
        Request request = new Request.Builder()
                .url(SEND_MESSAGE_URL)
                .addHeader("Authorization", "Bearer " + token)
                .post(requestBody)
                .build();
        Response response = client.newCall(request).execute();
        String json = response.body() != null ? response.body().string() : "";
        FeishuSendMessageResult result = JSON.parseObject(json, FeishuSendMessageResult.class);
        if (result != null && result.getCode() != 0) {
            log.warn("飞书单发消息发送失败 code={}, msg={}, receive_id={}", result.getCode(), result.getMsg(), message.getReceive_id());
        }
    }

    private String getTokenCacheKey(FeishuAppMessage message) {
        return message.getProjectId() + "-" + message.getAppId();
    }

    static class TokenRequest {
        private final String app_id;
        private final String app_secret;

        TokenRequest(String appId, String appSecret) {
            this.app_id = appId;
            this.app_secret = appSecret;
        }

        public String getApp_id() {
            return app_id;
        }

        public String getApp_secret() {
            return app_secret;
        }
    }

    static class LookupUserRequest {
        private final String[] mobiles;

        LookupUserRequest(String mobile) {
            this.mobiles = new String[]{mobile};
        }

        public String[] getMobiles() {
            return mobiles;
        }
    }
}
