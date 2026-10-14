package com.cat2bug.system.service;

import com.cat2bug.common.domain.im.DingProjectConfig;
import com.cat2bug.common.domain.im.EnterpriseWeChatProjectConfig;
import com.cat2bug.common.domain.im.FeishuProjectConfig;
import com.cat2bug.common.util.StringUtils;
import com.cat2bug.system.service.im.DingClient;
import com.cat2bug.system.service.im.FeishuClient;
import com.cat2bug.system.service.im.WeComClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * IM 配置测试消息发送（仅用于配置页面的 group-test / single-test 接口），与正式推送共用平台客户端。
 */
@ApplicationScoped
public class ImConfigTestService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Inject
    ImProjectConfigService imProjectConfigService;

    @Inject
    DingClient dingClient;

    @Inject
    FeishuClient feishuClient;

    @Inject
    WeComClient weComClient;

    public void sendDingGroupTest(Long projectId, Long memberId, Map<String, String> config) throws Exception {
        String hook = config.get("hook");
        if (StringUtils.isBlank(hook)) {
            throw new IllegalArgumentException("请配置群发 Hook 地址");
        }
        String key = config.get("key");
        String testMessage = "这是来自 Cat2Bug 的钉钉测试消息";
        if (StringUtils.isNotBlank(key)) {
            testMessage = testMessage + "，关键词：" + key;
        }
        dingClient.sendGroupText(hook, config.get("secret"), testMessage);
    }

    public void sendDingSingleTest(Long projectId, Long memberId, Map<String, String> config) throws Exception {
        String mobile = config.get("mobile");
        if (StringUtils.isBlank(mobile)) {
            throw new IllegalArgumentException("请配置单发用户手机号");
        }
        DingProjectConfig app = imProjectConfigService.selectAppConfig(
                projectId, DingProjectConfig.SYSTEM_CODE, DingProjectConfig.class);
        if (app == null) {
            throw new IllegalArgumentException("请先在项目配置中设置钉钉企业应用配置");
        }
        ObjectNode msgParams = MAPPER.createObjectNode();
        msgParams.put("title", "Cat2Bug 测试消息");
        msgParams.put("text", "这是来自 Cat2Bug 的钉钉测试消息");
        dingClient.sendRobotMessage(app, mobile, "sampleMarkdown", msgParams);
    }

    public void sendWeChatSingleTest(Long projectId, Long memberId, Map<String, String> config) throws Exception {
        String mobile = config.get("mobile");
        if (StringUtils.isBlank(mobile)) {
            throw new IllegalArgumentException("请配置单发手机号");
        }
        EnterpriseWeChatProjectConfig app = imProjectConfigService.selectAppConfig(
                projectId, EnterpriseWeChatProjectConfig.SYSTEM_CODE, EnterpriseWeChatProjectConfig.class);
        if (app == null) {
            throw new IllegalArgumentException("请先在项目配置中设置企业微信应用配置");
        }
        weComClient.sendAppMessage(app, mobile, "text", "这是来自 Cat2Bug 的企业微信测试消息");
    }

    public void sendWeChatGroupTest(Long projectId, Long memberId, Map<String, String> config) throws Exception {
        String hook = config.get("hook");
        if (StringUtils.isBlank(hook)) {
            throw new IllegalArgumentException("请配置群发 Webhook 地址");
        }
        weComClient.sendGroupMarkdown(hook, "这是来自 Cat2Bug 的企业微信群机器人测试消息", config.get("mobile"));
    }

    public void sendFeishuGroupTest(Long projectId, Long memberId, Map<String, String> config) throws Exception {
        String hook = config.get("hook");
        if (StringUtils.isBlank(hook)) {
            throw new IllegalArgumentException("请配置群发 Hook 地址");
        }
        String key = config.get("key");
        String testMessage = "这是来自 Cat2Bug 的飞书测试消息";
        if (StringUtils.isNotBlank(key)) {
            testMessage = testMessage + "，关键词：" + key;
        }
        feishuClient.sendGroupText(hook, config.get("secret"), testMessage);
    }

    public void sendFeishuSingleTest(Long projectId, Long memberId, Map<String, String> config) throws Exception {
        String mobile = config.get("mobile");
        if (StringUtils.isBlank(mobile)) {
            throw new IllegalArgumentException("请配置单发用户手机号");
        }
        FeishuProjectConfig app = imProjectConfigService.selectAppConfig(
                projectId, FeishuProjectConfig.SYSTEM_CODE, FeishuProjectConfig.class);
        if (app == null || StringUtils.isBlank(app.getAppId()) || StringUtils.isBlank(app.getAppSecret())) {
            throw new IllegalArgumentException("请先在项目配置中设置飞书企业应用 appId 和 appSecret");
        }
        feishuClient.sendUserText(app, mobile, "这是来自 Cat2Bug 的飞书测试消息");
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> extractConfigMap(Map<String, Object> params) {
        Object configObj = params.get("config");
        if (configObj instanceof Map<?, ?> map) {
            Map<String, String> result = new LinkedHashMap<>();
            map.forEach((k, v) -> result.put(String.valueOf(k), v != null ? String.valueOf(v) : null));
            return result;
        }
        throw new IllegalArgumentException("config 参数无效");
    }
}
