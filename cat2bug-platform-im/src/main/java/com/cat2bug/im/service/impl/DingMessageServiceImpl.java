package com.cat2bug.im.service.impl;

import com.alibaba.fastjson.JSON;
import com.cat2bug.common.core.redis.RedisCache;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.im.domain.DingErrorResponse;
import com.cat2bug.im.domain.DingMessage;
import com.cat2bug.im.domain.DingTokenResult;
import com.cat2bug.im.domain.IMDingPlatformConfig;
import com.cat2bug.im.service.IIMService;
import okhttp3.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-07-08 00:15
 * @Version: 1.0.0
 */
@Service
public class DingMessageServiceImpl implements IIMService<DingMessage, IMDingPlatformConfig> {
    private final static Logger log = LogManager.getLogger(DingMessageServiceImpl.class);
    public final static String MESSAGE_FACTORY_NAME = "ImDingMessage";
    /**
     * 获取token接口网址
     */
    private final static String GET_TOKEN_URL = "https://api.dingtalk.com/v1.0/oauth2/accessToken";
    /**
     * 通过手机号获取用户ID接口网址
     */
    private final static String GET_USER_BY_MOBILE_URL = "https://oapi.dingtalk.com/topapi/v2/user/getbymobile";
    /**
     * 发送内部机器人消息接口网址
     */
    private final static String SEND_INTERNAL_ROBOT_MESSAGE_URL = "https://api.dingtalk.com/v1.0/robot/oToMessages/batchSend";
    /** token缓存 */
    @Autowired
    private RedisCache redisCache;

    /** 存储钉钉机器人消息的缓存组名称 */
    private final static String DING_TOKEN_KEY = "ding-robot-message-token";

    /** 重发次数的缓存 */
    private Map<String, Integer> resendCountCache = new ConcurrentHashMap<>();
    /**
     * 接口内容类型
     */
    private static final MediaType FORM_CONTENT_TYPE = MediaType.parse("application/json;charset=utf-8");

    @Override
    public void sendNoticeMessage(DingMessage message, IMDingPlatformConfig config) throws Exception {
        OkHttpClient client = new OkHttpClient().newBuilder().readTimeout(30000, TimeUnit.SECONDS).build();
        // 发送hook群消息
        if(StringUtils.isNotBlank(config.getHook())) {
            this.sendHookMessage(client, message, config);
        }
        // 发送企业内部单人消息（只有当设置了 mobile 或 userIds 时才发送）
        if(StringUtils.isNotBlank(config.getMobile()) || (message.getUserIds() != null && !message.getUserIds().isEmpty())) {
            this.sendInternalMessage(client, message, config);
        }
    }

    /**
     * 发送企业内部单人消息
     * @param client
     * @param message
     * @param config
     * @throws Exception
     */
    private void sendInternalMessage(OkHttpClient client, DingMessage message, IMDingPlatformConfig config) throws Exception {
        String tokenKey = this.getTokenCacheKey(message);
        if(redisCache.hasKey(DING_TOKEN_KEY, tokenKey)==false) {
            String token = this.getToken(client, message);
            redisCache.setCacheObject(DING_TOKEN_KEY, tokenKey, token);
        }
        String accessToken = redisCache.getCacheObject(DING_TOKEN_KEY, tokenKey);

        // 如果配置了手机号，先通过手机号获取 User ID
        if(StringUtils.isNotBlank(config.getMobile())) {
            String userId = this.getUserIdByMobile(client, accessToken, config.getMobile(), message);
            if(message.getUserIds() == null || message.getUserIds().isEmpty()) {
                message.setUserIds(java.util.Arrays.asList(userId));
            }
        }

        // 如果 message.getUserIds() 中包含手机号格式（非钉钉 User ID），则转换为 User ID
        // 钉钉 User ID 通常不包含数字以外的字符，而手机号是纯数字
        // 这里假设如果 userIds 中的值看起来像手机号（纯数字且长度合适），则进行转换
        if(message.getUserIds() != null && !message.getUserIds().isEmpty()) {
            List<String> convertedUserIds = new java.util.ArrayList<>();
            for(String id : message.getUserIds()) {
                // 如果看起来像手机号（11位数字），则转换
                if(id != null && id.matches("\\d{11}")) {
                    String userId = this.getUserIdByMobile(client, accessToken, id, message);
                    convertedUserIds.add(userId);
                } else {
                    convertedUserIds.add(id);
                }
            }
            message.setUserIds(convertedUserIds);
        }

        sendInternalRobotMessage(client, accessToken, message);
    }

    private void sendInternalRobotMessage(OkHttpClient client, String token, DingMessage message) throws Exception {
        String body = JSON.toJSONString(message);
        RequestBody formBody = RequestBody.create(FORM_CONTENT_TYPE, body);
        Request request = new Request.Builder()
                .header("x-acs-dingtalk-access-token", token)
                .url(SEND_INTERNAL_ROBOT_MESSAGE_URL)
                .post(formBody).build();
        Response response = client.newCall(request).execute();
        String json = response.body().string();
        log.debug("钉钉机器人发送企业内部消息接口，发送数据:{} \n返回消息:{}", body, json);
        switch (response.code()) {
            case 200:
//              DingInternalRobotMessageResult result = JSON.parseObject(json, DingInternalRobotMessageResult.class);
                break;
            case 400:
                this.handleExceptionOfSendInternalRobotMessage(client, message, json);
                break;
        }
    }

    /**
     * 处理发送机器人消息后返回的异常错误
     * @param client
     * @param message
     * @param response
     * @throws Exception
     */
    private void handleExceptionOfSendInternalRobotMessage(OkHttpClient client, DingMessage message, String response) throws Exception {
        DingErrorResponse dingErrorResponse = JSON.parseObject(response, DingErrorResponse.class);
        switch (dingErrorResponse.getCode()) {
            // 无效鉴权
            case DingErrorResponse.INVALID_AUTHENTICATION_CODE:
                // 重新申请token
                String tokenKey = this.getTokenCacheKey(message);
                String token = this.getToken(client, message);
                redisCache.setCacheObject(DING_TOKEN_KEY, tokenKey, token);
                // 如果重发次数大于0，测发送
                if(this.refreshResendCountCache(tokenKey)>0) {
                    sendInternalRobotMessage(client, token, message);// 重新发送信息
                }
                break;
        }
    }

    /**
     * 获取Token
     * @param client
     * @param message
     * @return
     * @throws Exception
     */
    private String getToken(OkHttpClient client, DingMessage message) throws Exception {
        Map<String,Object> params = new HashMap<>();
        params.put("appKey",message.getAppKey());
        params.put("appSecret",message.getAppSecret());
        String body = JSON.toJSONString(params);
        RequestBody formBody = RequestBody.create(FORM_CONTENT_TYPE, body);
        Request request = new Request.Builder()
                .url(GET_TOKEN_URL)
                .post(formBody).build();
        Response response = client.newCall(request).execute();
        String json = response.body().string();
        DingTokenResult tokenResult = JSON.parseObject(json, DingTokenResult.class);
        log.debug("钉钉获取token接口返回消息:{}",json);
        return tokenResult.getAccessToken();
    }

    /**
     * 发送hook群消息
     * @param message
     * @param config
     * @throws Exception
     */
    private void sendHookMessage(OkHttpClient client, DingMessage message, IMDingPlatformConfig config) throws Exception {
        message.getText().setContent(message.getText().getContent());
        String body = JSON.toJSONString(message);
        RequestBody formBody = RequestBody.create(FORM_CONTENT_TYPE, body);

        // 构建请求URL，如果配置了secret则添加签名参数
        String url = config.getHook();
        if (StringUtils.isNotBlank(config.getSecret())) {
            long timestamp = System.currentTimeMillis();
            String sign = generateSign(timestamp, config.getSecret());
            // 判断URL是否已有参数
            String separator = url.contains("?") ? "&" : "?";
            url = url + separator + "timestamp=" + timestamp + "&sign=" + sign;
        }

        Request request = new Request.Builder()
                .url(url)
                .post(formBody).build();
        Response response = client.newCall(request).execute();
        String json = response.body().string();
        log.debug("钉钉消息已经发送。{}",json);
        if(response.isSuccessful()==false) {
            log.error("发送钉钉信息错误,返回信息:{} \n 请求参数:{}",json, body);
        }
    }

    /**
     * 生成钉钉加签
     * @param timestamp 时间戳
     * @param secret 密钥
     * @return 签名
     * @throws Exception
     */
    private String generateSign(long timestamp, String secret) throws Exception {
        String stringToSign = timestamp + "\n" + secret;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA256"));
        byte[] signData = mac.doFinal(stringToSign.getBytes("UTF-8"));
        String sign = java.util.Base64.getEncoder().encodeToString(signData);
        return java.net.URLEncoder.encode(sign, "UTF-8");
    }

    @Override
    public String getMessageFactoryName() {
        return MESSAGE_FACTORY_NAME;
    }

    @Override
    public void receiveMessage(DingMessage message) {

    }

    /**
     * 获取token缓存的key
     * @param message
     * @return
     */
    private String getTokenCacheKey(DingMessage message) {
        return message.getProjectId() + "-" + message.getAppKey();
    }

    /**
     * 刷新重发次数
     * @param tokenKey  token值
     * @return          剩余发送次数
     */
    private int refreshResendCountCache(String tokenKey) {
        if(resendCountCache.containsKey(tokenKey)==false) {
            resendCountCache.put(tokenKey, 3);
        } else {
            resendCountCache.put(tokenKey, resendCountCache.get(tokenKey) - 1);
        }
        int count = resendCountCache.get(tokenKey); // 剩余刷新次数
        if(count<=0) {
            resendCountCache.remove(tokenKey);
            return 0;
        } else {
            return resendCountCache.get(tokenKey);
        }
    }

    /**
     * 通过手机号获取钉钉用户ID
     * 注意：钉钉API会优先查找企业手机号，如果企业手机号未设置，会自动使用用户的个人手机号进行查找
     * @param client OkHttpClient
     * @param accessToken 访问令牌
     * @param mobile 手机号（企业手机号或个人手机号）
     * @param message DingMessage（用于token刷新）
     * @return 用户ID
     * @throws Exception
     */
    private String getUserIdByMobile(OkHttpClient client, String accessToken, String mobile, DingMessage message) throws Exception {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("mobile", mobile);

        RequestBody body = RequestBody.create(FORM_CONTENT_TYPE, JSON.toJSONString(requestBody));
        Request request = new Request.Builder()
                .url(GET_USER_BY_MOBILE_URL + "?access_token=" + accessToken)
                .post(body)
                .build();

        Response response = client.newCall(request).execute();
        String responseBody = response.body().string();
        log.info("通过手机号获取用户ID响应: {}", responseBody);

        Map<String, Object> result = JSON.parseObject(responseBody, Map.class);
        Integer errcode = (Integer) result.get("errcode");

        if (errcode != null && errcode == 0) {
            Map<String, Object> resultData = (Map<String, Object>) result.get("result");
            return (String) resultData.get("userid");
        } else if (errcode != null && (errcode == 40014 || errcode == 88)) {
            // Token 无效（errcode=40014 或 88），刷新 token 并重试
            log.warn("获取用户ID时token无效(errcode={}，尝试刷新token并重试", errcode);
            String tokenKey = this.getTokenCacheKey(message);
            String newToken = this.getToken(client, message);
            redisCache.setCacheObject(DING_TOKEN_KEY, tokenKey, newToken);

            // 重试一次
            return getUserIdByMobileRetry(client, newToken, mobile);
        } else {
            throw new Exception("获取用户ID失败: " + result.get("errmsg"));
        }
    }

    /**
     * 重试获取用户ID（不再处理token失效）
     */
    private String getUserIdByMobileRetry(OkHttpClient client, String accessToken, String mobile) throws Exception {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("mobile", mobile);

        RequestBody body = RequestBody.create(FORM_CONTENT_TYPE, JSON.toJSONString(requestBody));
        Request request = new Request.Builder()
                .url(GET_USER_BY_MOBILE_URL + "?access_token=" + accessToken)
                .post(body)
                .build();

        Response response = client.newCall(request).execute();
        String responseBody = response.body().string();
        log.info("重试通过手机号获取用户ID响应: {}", responseBody);

        Map<String, Object> result = JSON.parseObject(responseBody, Map.class);
        if (result.get("errcode") != null && (Integer) result.get("errcode") == 0) {
            Map<String, Object> resultData = (Map<String, Object>) result.get("result");
            return (String) resultData.get("userid");
        } else {
            throw new Exception("获取用户ID失败: " + result.get("errmsg"));
        }
    }
}
