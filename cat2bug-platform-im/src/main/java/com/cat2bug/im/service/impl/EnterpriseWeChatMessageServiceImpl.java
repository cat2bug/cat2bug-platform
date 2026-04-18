package com.cat2bug.im.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.im.domain.*;
import com.cat2bug.im.service.IIMProjectConfigService;
import com.cat2bug.im.service.IIMService;
import okhttp3.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
    /** 通过手机号获取userid网址 */
    private final static String GET_USERID_BY_MOBILE_URL = "https://qyapi.weixin.qq.com/cgi-bin/user/getuserid?access_token=%s";
    /** 发送应用消息网址 */
    private final static String SEND_MESSAGE_URL = "https://qyapi.weixin.qq.com/cgi-bin/message/send?access_token=%s";
    /** 接口内容类型 */
    private static final MediaType FORM_CONTENT_TYPE = MediaType.parse("application/json;charset=utf-8");
    /** token无效的错误返回码 */
    private static final int INVALID_ACCESS_TOKEN_ERR_CODE = 40014;

    /** token缓存 */
    private final Map<String, String> tokenCache = new ConcurrentHashMap<>();

    @Autowired
    private IIMProjectConfigService imProjectConfigService;

    @Override
    public String getMessageFactoryName() {
        return MESSAGE_FACTORY_NAME;
    }

    @Override
    public void sendNoticeMessage(EnterpriseWeChatAppMessage message, EnterpriseWeChatPlatformConfig config) throws Exception {
        if (Boolean.TRUE.equals(config.getGroupSwitch())) {
            sendGroupNoticeMessage(message, config);
            return;
        }
        if (Boolean.TRUE.equals(config.getSingleSwitch())) {
            sendSingleNoticeMessage(message, config);
        }
    }

    public void sendSingleNoticeMessage(EnterpriseWeChatAppMessage message, EnterpriseWeChatPlatformConfig config) throws Exception {
        if(StringUtils.isBlank(config.getMobile())) {
            log.warn("企业微信手机号为空，无法发送单发信息 message:{}",JSON.toJSONString(message));
            return;
        }
        OkHttpClient client = new OkHttpClient().newBuilder().readTimeout(10000, TimeUnit.SECONDS).build();
        String token = getToken(client, message);
        String userId = getUserIdByMobile(client, token, config.getMobile());
        if(StringUtils.isBlank(userId)) {
            log.warn("通过手机号未查询到企业微信用户ID，mobile={}", config.getMobile());
            return;
        }
        message.setTouser(userId);
        this.sendMessage(client, token, message);
    }

    public void sendGroupNoticeMessage(EnterpriseWeChatAppMessage message, EnterpriseWeChatPlatformConfig config) throws Exception {
        if (StringUtils.isBlank(config.getHook())) {
            log.warn("企业微信群机器人 Webhook 为空，跳过群发 message:{}", JSON.toJSONString(message));
            return;
        }
        OkHttpClient client = new OkHttpClient().newBuilder().readTimeout(10000, TimeUnit.SECONDS).build();
        JSONObject body = new JSONObject();
        body.put("msgtype", "markdown");
        JSONObject markdown = new JSONObject();
        String content = message.getContent();
        if (StringUtils.isBlank(content) && message instanceof TextEnterpriseWeChatAppMessage) {
            TextEnterpriseWeChatAppMessage msg = (TextEnterpriseWeChatAppMessage) message;
            if (msg.getMarkdown() != null) {
                content = msg.getMarkdown().getContent();
            }
        }
        markdown.put("content", StringUtils.isBlank(content) ? "企业微信群消息" : content);
        body.put("markdown", markdown);
        if (StringUtils.isNotBlank(config.getMobile())) {
            body.put("mentioned_mobile_list", new String[]{config.getMobile()});
        }
        RequestBody formBody = RequestBody.create(FORM_CONTENT_TYPE, body.toJSONString());
        Request request = new Request.Builder().url(config.getHook()).post(formBody).build();
        Response response = client.newCall(request).execute();
        String json = response.body() != null ? response.body().string() : "";
        log.info("企业微信群机器人发送消息请求体：{}", body.toJSONString());
        log.info("企业微信群机器人发送消息返回数据：{}", json);
    }

    public void sendSingleTestMessage(Long projectId, Long memberId, EnterpriseWeChatPlatformConfig config) throws Exception {
        IMProjectConfig projectConfig = imProjectConfigService.selectImProjectConfigByProjectIdAndSystemCode(projectId, EnterpriseWeChatProjectConfig.SYSTEM_CODE);
        EnterpriseWeChatProjectConfig weChatConfig = JSON.parseObject(projectConfig.getConfigParams(), EnterpriseWeChatProjectConfig.class);
        TextEnterpriseWeChatAppMessage message = new TextEnterpriseWeChatAppMessage("这是来自 Cat2Bug 的企业微信测试消息");
        message.setProjectId(projectId);
        message.setReceiveMemberId(memberId);
        message.setAgentid(Integer.valueOf(weChatConfig.getAgentid()));
        message.setCorpId(weChatConfig.getCorpId());
        message.setCorpSecret(weChatConfig.getCorpSecret());
        sendSingleNoticeMessage(message, config);
    }

    public void sendGroupTestMessage(Long projectId, Long memberId, EnterpriseWeChatPlatformConfig config) throws Exception {
        TextEnterpriseWeChatAppMessage message = new TextEnterpriseWeChatAppMessage("这是来自 Cat2Bug 的企业微信群机器人测试消息");
        message.setProjectId(projectId);
        message.setReceiveMemberId(memberId);
        sendGroupNoticeMessage(message, config);
    }

    @Override
    public void receiveMessage(EnterpriseWeChatAppMessage message) {
        // TODO 暂时不实现企业微信接收消息
    }

    private String getToken(OkHttpClient client, EnterpriseWeChatAppMessage message) throws Exception {
        String tokenCacheKey = this.getTokenCacheKey(message);
        if(this.tokenCache.containsKey(tokenCacheKey)) {
            return this.tokenCache.get(tokenCacheKey);
        }
        String url = String.format(GET_TOKEN_URL, message.getCorpId(), message.getCorpSecret());
        Request request = new Request.Builder().url(url).build();
        Response response = client.newCall(request).execute();
        String json = response.body() != null ? response.body().string() : "";
        EnterpriseWeChatTokenResult result = JSON.parseObject(json, EnterpriseWeChatTokenResult.class);
        if(result.getErrcode()==0) {
            tokenCache.put(tokenCacheKey, result.getAccess_token());
            return result.getAccess_token();
        } else {
            throw new Exception("获取企业微信token异常:" + json);
        }
    }

    private String getUserIdByMobile(OkHttpClient client, String token, String mobile) throws Exception {
        String url = String.format(GET_USERID_BY_MOBILE_URL, token);
        JSONObject body = new JSONObject();
        body.put("mobile", mobile);
        RequestBody formBody = RequestBody.create(FORM_CONTENT_TYPE, body.toJSONString());
        Request request = new Request.Builder().url(url).post(formBody).build();
        Response response = client.newCall(request).execute();
        String json = response.body() != null ? response.body().string() : "";
        JSONObject result = JSON.parseObject(json);
        Integer errcode = result.getInteger("errcode");
        if(errcode != null && errcode == 0) {
            return result.getString("userid");
        } else if (errcode != null && errcode == 60020) {
            throw new Exception("企业微信接口拒绝当前服务器IP访问，请在企业微信应用配置中添加可信IP");
        } else {
            throw new Exception("通过手机号获取企业微信userid失败:" + json);
        }
    }

    private void sendMessage(OkHttpClient client, String token, EnterpriseWeChatAppMessage message) throws IOException {
        String url = String.format(SEND_MESSAGE_URL,token);
        String body = JSON.toJSONString(message);
        RequestBody formBody = RequestBody.create(FORM_CONTENT_TYPE, body);
        Request request = new Request.Builder()
                .url(url)
                .post(formBody).build();
        Response response = client.newCall(request).execute();
        String json = response.body() != null ? response.body().string() : "";
        EnterpriseWeChatSendMessageResult result = JSON.parseObject(json, EnterpriseWeChatSendMessageResult.class);
        if(result.getErrcode()==INVALID_ACCESS_TOKEN_ERR_CODE) {
            this.tokenCache.remove(this.getTokenCacheKey(message));
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
