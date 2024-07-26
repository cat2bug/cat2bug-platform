package com.cat2bug.im.service.impl;

import com.alibaba.fastjson.JSON;
import com.cat2bug.im.domain.EnterpriseWeChatAppMessage;
import com.cat2bug.im.domain.EnterpriseWeChatPlatformConfig;
import com.cat2bug.im.domain.EnterpriseWeChatResult;
import com.cat2bug.im.domain.TextEnterpriseWeChatAppMessage;
import com.cat2bug.im.service.IIMService;
import okhttp3.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-07-26 16:56
 * @Version: 1.0.0
 */
@Service
public class EnterpriseWeChatMessageServiceImpl implements IIMService<EnterpriseWeChatAppMessage, EnterpriseWeChatPlatformConfig> {
    private final static Logger log = LogManager.getLogger(EnterpriseWeChatMessageServiceImpl.class);
    /** 获取token网址 */
    private final static String GET_TOKEN_URL = "https://qyapi.weixin.qq.com/cgi-bin/gettoken?corpid=%s&corpsecret=%s";
    /** 发送应用消息网址 */
    private final static String SEND_MESSAGE_URL = "https://qyapi.weixin.qq.com/cgi-bin/message/send?access_token=%s";
    /** 接口内容类型 */
    private static final MediaType FORM_CONTENT_TYPE = MediaType.parse("application/json;charset=utf-8");
    /** 企业ID */
    private String corpid="wwb7ca5b34cb6be74d";
    /** 应用的凭证密钥 */
    private String corpsecret="asCk6FoH2guMYZkzVhgirOiimjRWQU8txn6oOQUQ9rM";

    @PostConstruct
    public void test() throws Exception {
        TextEnterpriseWeChatAppMessage message = new TextEnterpriseWeChatAppMessage("这是一个测试用例");
        message.setTouser("YuZhanTao");
        message.setAgentid(1000002);
        sendNoticeMessage(message, null);
    }

    @Override
    public String getMessageFactoryName() {
        return "ImEnterpriseWeChatMessage";
    }

    @Override
    public void sendNoticeMessage(EnterpriseWeChatAppMessage message, EnterpriseWeChatPlatformConfig config) throws Exception {
        String url = String.format(GET_TOKEN_URL,this.corpid,this.corpsecret);
        OkHttpClient client = new OkHttpClient().newBuilder().readTimeout(10000, TimeUnit.SECONDS).build();
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {

            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                String json = response.body().string();
                log.info("获取token:{}",json);
                EnterpriseWeChatResult result = JSON.parseObject(json, EnterpriseWeChatResult.class);
                sendMessage(client,result.getAccess_token(),message);
            }
        });
    }

    @Override
    public void receiveMessage(EnterpriseWeChatAppMessage message) {


    }

    private void sendMessage(OkHttpClient client, String token, EnterpriseWeChatAppMessage message) throws IOException {
//        message.getText().setContent(message.getText().getContent());
        String url = String.format(SEND_MESSAGE_URL,token);
        String body = JSON.toJSONString(message);
        RequestBody formBody = RequestBody.create(FORM_CONTENT_TYPE, body);
        Request request = new Request.Builder()
                .url(url)
                .post(formBody).build();
        Response response = client.newCall(request).execute();
        String json = response.body().string();
        log.info("微信企业发送信息请求体：{}",body);
        log.info("微信企业发送信息返回数据：{}",json);
    }
}
