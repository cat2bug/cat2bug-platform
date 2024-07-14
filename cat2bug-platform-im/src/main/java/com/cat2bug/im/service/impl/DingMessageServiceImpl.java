package com.cat2bug.im.service.impl;

import com.alibaba.fastjson.JSON;
import com.cat2bug.im.domain.DingMessage;
import com.cat2bug.im.service.IIMService;
import okhttp3.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-07-08 00:15
 * @Version: 1.0.0
 */
@Service
public class DingMessageServiceImpl implements IIMService<DingMessage> {
    private final static Logger log = LogManager.getLogger(DingMessageServiceImpl.class);
    public final static String MESSAGE_FACTORY_NAME = "ImDingMessage";
    /**
     * 接口内容类型
     */
    private static final MediaType FORM_CONTENT_TYPE = MediaType.parse("application/json;charset=utf-8");

    @Override
    public void sendNoticeMessage(DingMessage message) throws Exception {
        OkHttpClient client = new OkHttpClient().newBuilder().readTimeout(10000, TimeUnit.SECONDS).build();
        message.getText().setContent("test"+message.getText().getContent());
        String body = JSON.toJSONString(message);
        RequestBody formBody = RequestBody.create(FORM_CONTENT_TYPE, body);
        Request request = new Request.Builder()
                .url(message.getWebHook())
                .post(formBody).build();
        Response response = client.newCall(request).execute();
        String json = response.body().string();
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
}
