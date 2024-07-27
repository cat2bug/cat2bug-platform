package com.cat2bug.im.service.impl;

import com.alibaba.fastjson.JSON;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.im.domain.*;
import com.cat2bug.im.service.IIMProjectConfigService;
import com.cat2bug.im.service.IIMService;
import okhttp3.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-07-26 16:56
 * @Version: 1.0.0
 */
@Service
public class EnterpriseWeChatMessageServiceImpl implements IIMService<EnterpriseWeChatAppMessage, EnterpriseWeChatPlatformConfig> {
    public final static String MESSAGE_FACTORY_NAME = "ImEnterpriseWeChatMessage";

    private final static Logger log = LogManager.getLogger(EnterpriseWeChatMessageServiceImpl.class);
    /** 获取token网址 */
    private final static String GET_TOKEN_URL = "https://qyapi.weixin.qq.com/cgi-bin/gettoken?corpid=%s&corpsecret=%s";
    /** 发送应用消息网址 */
    private final static String SEND_MESSAGE_URL = "https://qyapi.weixin.qq.com/cgi-bin/message/send?access_token=%s";
    /** 接口内容类型 */
    private static final MediaType FORM_CONTENT_TYPE = MediaType.parse("application/json;charset=utf-8");
    /** token无效的错误返回码 */
    private static final int INVALID_ACCESS_TOKEN_ERR_CODE = 40014;

    /** token缓存 */
    private Map<String, String> tokenCache = new ConcurrentHashMap<>();

    @Override
    public String getMessageFactoryName() {
        return MESSAGE_FACTORY_NAME;
    }

    @Override
    public void sendNoticeMessage(EnterpriseWeChatAppMessage message, EnterpriseWeChatPlatformConfig config) throws Exception {
        if(StringUtils.isBlank(config.getUserId())) {
            log.warn("企业微信用户ID为空，无法发送信息 message:{}",JSON.toJSONString(message));
            return;
        }
        message.setTouser(config.getUserId());
        OkHttpClient client = new OkHttpClient().newBuilder().readTimeout(10000, TimeUnit.SECONDS).build();
        String tokenCacheKey = this.getTokenCacheKey(message);
        String token = null;
        if(this.tokenCache.containsKey(tokenCacheKey)) {
            token = this.tokenCache.get(tokenCacheKey);
        }
        if(StringUtils.isNotBlank(token)) {
            this.sendMessage(client, token, message);
        } else {
            this.getTokenAndSendMessage(client,message);
        }
    }

    @Override
    public void receiveMessage(EnterpriseWeChatAppMessage message) {
        // TODO 暂时不实现企业微信接收消息
    }

    private void getTokenAndSendMessage(OkHttpClient client, EnterpriseWeChatAppMessage message) {
        // 调用获取token接口
        String url = String.format(GET_TOKEN_URL, message.getCorpId(), message.getCorpSecret());
        Request request = new Request.Builder().url(url).build();
        String tokenCacheKey = this.getTokenCacheKey(message);
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {

            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                String json = response.body().string();
                EnterpriseWeChatTokenResult result = JSON.parseObject(json, EnterpriseWeChatTokenResult.class);
                if(result.getErrcode()==0) {
                    tokenCache.put(tokenCacheKey, result.getAccess_token());
                    sendMessage(client, result.getAccess_token(), message);
                } else {
                    log.warn("获取企业微信token异常:{}", json);
                }
            }
        });
    }

    private void sendMessage(OkHttpClient client, String token, EnterpriseWeChatAppMessage message) throws IOException {
        String url = String.format(SEND_MESSAGE_URL,token);
        String body = JSON.toJSONString(message);
        RequestBody formBody = RequestBody.create(FORM_CONTENT_TYPE, body);
        Request request = new Request.Builder()
                .url(url)
                .post(formBody).build();
        Response response = client.newCall(request).execute();
        String json = response.body().string();
        EnterpriseWeChatSendMessageResult result = JSON.parseObject(json, EnterpriseWeChatSendMessageResult.class);
        // token过期处理
        if(result.getErrcode()==INVALID_ACCESS_TOKEN_ERR_CODE) {
            this.getTokenAndSendMessage(client,message);
        }
        log.info("微信企业发送信息请求体：{}",body);
        log.info("微信企业发送信息返回数据：{}",json);
    }

    /**
     * 获取token缓存的key
     * @param message
     * @return
     */
    private String getTokenCacheKey(EnterpriseWeChatAppMessage message) {
        return message.getProjectId() + "-" + message.getAgentid();
    }
}
