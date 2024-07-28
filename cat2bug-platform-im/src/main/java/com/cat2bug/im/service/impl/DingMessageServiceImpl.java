package com.cat2bug.im.service.impl;

import com.alibaba.fastjson.JSON;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.im.domain.DingMessage;
import com.cat2bug.im.domain.DingTokenResult;
import com.cat2bug.im.domain.IMDingPlatformConfig;
import com.cat2bug.im.service.IIMService;
import lombok.extern.log4j.Log4j;
import okhttp3.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.util.HashMap;
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
     * 发送内部机器人消息接口网址
     */
    private final static String SEND_INTERNAL_ROBOT_MESSAGE_URL = "https://api.dingtalk.com/v1.0/robot/oToMessages/batchSend";
    /** token缓存 */
    private Map<String, String> tokenCache = new ConcurrentHashMap<>();
    /**
     * 接口内容类型
     */
    private static final MediaType FORM_CONTENT_TYPE = MediaType.parse("application/json;charset=utf-8");

    @Override
    public void sendNoticeMessage(DingMessage message, IMDingPlatformConfig config) throws Exception {
        OkHttpClient client = new OkHttpClient().newBuilder().readTimeout(10000, TimeUnit.SECONDS).build();
        // 发送hook群消息
        if(StringUtils.isNotBlank(config.getHook())) {
            this.sendHookMessage(client, message, config);
        }
        // 发送企业内部单人消息
        this.sendInternalMessage(client, message, config);
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
        if(tokenCache.containsKey(tokenKey)==false) {
            String token = this.getToken(client, message);
            tokenCache.put(tokenKey, token);
        }
        sendInternalRobotMessage(client, tokenCache.get(tokenKey),message,config);
    }

    private void sendInternalRobotMessage(OkHttpClient client, String token, DingMessage message, IMDingPlatformConfig config) throws Exception {
        String body = JSON.toJSONString(message);
        RequestBody formBody = RequestBody.create(FORM_CONTENT_TYPE, body);
        Request request = new Request.Builder()
                .header("x-acs-dingtalk-access-token", token)
                .url(SEND_INTERNAL_ROBOT_MESSAGE_URL)
                .post(formBody).build();
        Response response = client.newCall(request).execute();
        String json = response.body().string();
//        DingInternalRobotMessageResult result = JSON.parseObject(json, DingInternalRobotMessageResult.class);
        log.info("钉钉机器人发送企业内部消息接口，发送数据:{} \n返回消息:{}",body, json);
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
        log.info("钉钉获取token接口返回消息:{}",json);
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
        Request request = new Request.Builder()
                .url(config.getHook())
                .post(formBody).build();
        Response response = client.newCall(request).execute();
        String json = response.body().string();
        log.info("钉钉消息已经发送。{}",json);
        if(response.isSuccessful()==false) {
            log.error("发送钉钉信息错误,返回信息:{} \n 请求参数:{}",json, body);
        }
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
}
