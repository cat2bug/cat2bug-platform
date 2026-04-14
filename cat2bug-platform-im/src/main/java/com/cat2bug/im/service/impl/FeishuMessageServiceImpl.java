package com.cat2bug.im.service.impl;

import com.alibaba.fastjson.JSON;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.im.domain.FeishuMessage;
import com.cat2bug.im.domain.IMBasePlatformConfig;
import com.cat2bug.im.service.IIMService;
import okhttp3.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-07-18 00:31
 * @Version: 1.0.0
 * 飞书自定义机器人消息发送服务（Webhook 方式）
 * 参考：https://open.feishu.cn/document/client-docs/bot-v3/add-custom-bot
 */
@Service
public class FeishuMessageServiceImpl implements IIMService<FeishuMessage, IMBasePlatformConfig> {
    private final static Logger log = LogManager.getLogger(FeishuMessageServiceImpl.class);

    public final static String MESSAGE_FACTORY_NAME = "ImFeishuMessage";

    private static final MediaType JSON_CONTENT_TYPE = MediaType.parse("application/json;charset=utf-8");

    @Override
    public String getMessageFactoryName() {
        return MESSAGE_FACTORY_NAME;
    }

    @Override
    public void sendNoticeMessage(FeishuMessage message, IMBasePlatformConfig config) throws Exception {
        // hook/secret 由 Factory 从项目级配置注入到 message 中
        String hook = message.getHook();
        String secret = message.getSecret();

        if (StringUtils.isBlank(hook)) {
            log.warn("飞书 Webhook 地址为空，跳过发送 message:{}", JSON.toJSONString(message));
            return;
        }

        OkHttpClient client = new OkHttpClient().newBuilder()
                .readTimeout(30, TimeUnit.SECONDS)
                .connectTimeout(10, TimeUnit.SECONDS)
                .build();

        // 构建 JSON body
        String body = buildRequestBody(message, secret);

        RequestBody requestBody = RequestBody.create(JSON_CONTENT_TYPE, body);
        Request.Builder requestBuilder = new Request.Builder()
                .url(hook)
                .post(requestBody);

        Response response = client.newCall(requestBuilder.build()).execute();
        String responseBody = response.body() != null ? response.body().string() : "";
        log.debug("飞书机器人发送消息，请求体:{} \n返回:{}", body, responseBody);

        if (!response.isSuccessful()) {
            log.error("飞书消息发送失败，HTTP状态码:{}, 返回:{}", response.code(), responseBody);
        }
    }

    @Override
    public void receiveMessage(FeishuMessage message) {
        // 暂不实现接收消息
    }

    /**
     * 构建请求 JSON body，只包含飞书 Webhook 所需字段
     * 若有签名密钥，将 timestamp 和 sign 嵌入请求体
     */
    private String buildRequestBody(FeishuMessage message, String secret) throws Exception {
        java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
        if (StringUtils.isNotBlank(secret)) {
            long timestamp = System.currentTimeMillis() / 1000;
            String sign = generateSign(secret, timestamp);
            body.put("timestamp", String.valueOf(timestamp));
            body.put("sign", sign);
        }
        body.put("msg_type", message.getMsg_type());
        body.put("content", message.getContent());
        return JSON.toJSONString(body);
    }

    /**
     * 生成飞书签名
     * 签名算法：Base64(HMAC-SHA256(timestamp + "\n" + secret))
     */
    private String generateSign(String secret, long timestamp) throws Exception {
        String stringToSign = timestamp + "\n" + secret;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(stringToSign.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] signData = mac.doFinal(new byte[]{});
        return Base64.getEncoder().encodeToString(signData);
    }
}
