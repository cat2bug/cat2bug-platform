package com.cat2bug.im.service.impl;

import com.alibaba.fastjson.JSON;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.im.domain.DingMessage;
import com.cat2bug.im.domain.IMDingPlatformConfig;
import com.cat2bug.im.service.IIMService;
import lombok.extern.log4j.Log4j;
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
@Log4j
public class DingMessageServiceImpl implements IIMService<DingMessage, IMDingPlatformConfig> {
    private final static Logger log = LogManager.getLogger(DingMessageServiceImpl.class);
    public final static String MESSAGE_FACTORY_NAME = "ImDingMessage";
    /**
     * 接口内容类型
     */
    private static final MediaType FORM_CONTENT_TYPE = MediaType.parse("application/json;charset=utf-8");

    @Override
    public void sendNoticeMessage(DingMessage message, IMDingPlatformConfig config) throws Exception {
        if(StringUtils.isBlank(config.getHook())) {
            throw new RuntimeException("钉钉Hook不能为空");
        }
        OkHttpClient client = new OkHttpClient().newBuilder().readTimeout(10000, TimeUnit.SECONDS).build();
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
}
