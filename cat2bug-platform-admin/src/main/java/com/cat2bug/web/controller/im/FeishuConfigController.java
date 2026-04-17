package com.cat2bug.web.controller.im;

import com.cat2bug.common.annotation.Log;
import com.cat2bug.common.core.controller.BaseController;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.enums.BusinessType;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.im.domain.FeishuAppMessage;
import com.cat2bug.im.domain.FeishuMessage;
import com.cat2bug.im.domain.FeishuProjectConfig;
import com.cat2bug.im.domain.IMProjectConfig;
import com.cat2bug.im.service.IIMProjectConfigService;
import com.cat2bug.im.service.impl.FeishuAppMessageServiceImpl;
import com.cat2bug.im.service.impl.FeishuMessageServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 飞书在项目中的配置Controller
 *
 * @author yuzhantao
 * @date 2024-07-18
 */
@RestController
@RequestMapping("/im/project/feishu/config")
public class FeishuConfigController extends BaseController {

    @Autowired
    private IIMProjectConfigService imProjectConfigService;

    @Autowired
    private FeishuMessageServiceImpl feishuMessageService;

    @Autowired
    private FeishuAppMessageServiceImpl feishuAppMessageService;

    /**
     * 获取飞书配置详细信息
     */
    @PreAuthorize("@ss.hasPermi('feishu:query')")
    @GetMapping
    public AjaxResult getInfo(@RequestParam("projectId") Long projectId) {
        return success(imProjectConfigService.selectImProjectConfigByProjectIdAndSystemCode(
                projectId,
                FeishuProjectConfig.SYSTEM_CODE));
    }

    /**
     * 保存飞书配置
     */
    @PreAuthorize("@ss.hasPermi('feishu:save')")
    @Log(title = "项目通知配置", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult save(@RequestBody IMProjectConfig imProjectConfig) {
        IMProjectConfig config = imProjectConfigService.selectImProjectConfigByProjectIdAndSystemCode(
                imProjectConfig.getProjectId(),
                FeishuProjectConfig.SYSTEM_CODE);

        imProjectConfig.setSystemCode(FeishuProjectConfig.SYSTEM_CODE);
        if (config == null) {
            return toAjax(imProjectConfigService.insertImProjectConfig(imProjectConfig));
        } else {
            imProjectConfig.setConfigId(config.getConfigId());
            return toAjax(imProjectConfigService.updateImProjectConfig(imProjectConfig));
        }
    }

    /**
     * 群发测试飞书通知
     */
    @PreAuthorize("@ss.hasPermi('feishu:query')")
    @PostMapping("/group-test")
    public AjaxResult groupTest(@RequestBody Map<String, Object> params) {
        try {
            Long projectId = Long.valueOf(params.get("projectId").toString());
            Long memberId = Long.valueOf(params.get("memberId").toString());
            @SuppressWarnings("unchecked")
            Map<String, String> config = (Map<String, String>) params.get("config");

            String key = config.get("key");
            String secret = config.get("secret");
            String hook = config.get("hook");

            if (StringUtils.isBlank(hook)) {
                return error("请配置群发 Hook 地址");
            }

            String testMessage = "这是来自 Cat2Bug 的飞书测试消息";
            if (StringUtils.isNotBlank(key)) {
                testMessage = testMessage + "，关键词：" + key;
            }

            FeishuMessage message = new FeishuMessage(testMessage);
            message.setProjectId(projectId);
            message.setReceiveMemberId(memberId);
            message.setHook(hook);
            message.setSecret(secret);
            feishuMessageService.sendNoticeMessage(message, null);
            return success("测试消息已发送到飞书群机器人");
        } catch (Exception e) {
            logger.error("飞书群发测试消息发送失败", e);
            return error("测试消息发送失败: " + e.getMessage());
        }
    }

    /**
     * 单发测试飞书通知
     */
    @PreAuthorize("@ss.hasPermi('feishu:query')")
    @PostMapping("/single-test")
    public AjaxResult singleTest(@RequestBody Map<String, Object> params) {
        try {
            Long projectId = Long.valueOf(params.get("projectId").toString());
            Long memberId = Long.valueOf(params.get("memberId").toString());
            @SuppressWarnings("unchecked")
            Map<String, String> config = (Map<String, String>) params.get("config");

            String mobile = config.get("mobile");
            if (StringUtils.isBlank(mobile)) {
                return error("请配置单发用户手机号");
            }

            IMProjectConfig projectConfig = imProjectConfigService.selectImProjectConfigByProjectIdAndSystemCode(projectId, FeishuProjectConfig.SYSTEM_CODE);
            if (projectConfig == null || StringUtils.isBlank(projectConfig.getConfigParams())) {
                return error("请先在项目配置中设置飞书企业应用 appId 和 appSecret");
            }
            FeishuProjectConfig feishuProjectConfig = com.alibaba.fastjson.JSON.parseObject(projectConfig.getConfigParams(), FeishuProjectConfig.class);
            if (StringUtils.isBlank(feishuProjectConfig.getAppId()) || StringUtils.isBlank(feishuProjectConfig.getAppSecret())) {
                return error("请先在项目配置中设置飞书企业应用 appId 和 appSecret");
            }

            FeishuAppMessage message = new FeishuAppMessage(null, "这是来自 Cat2Bug 的飞书测试消息");
            message.setProjectId(projectId);
            message.setReceiveMemberId(memberId);
            message.setAppId(feishuProjectConfig.getAppId());
            message.setAppSecret(feishuProjectConfig.getAppSecret());

            com.cat2bug.im.domain.FeishuPlatformConfig platformConfig = new com.cat2bug.im.domain.FeishuPlatformConfig();
            platformConfig.setMobile(mobile);
            feishuAppMessageService.sendNoticeMessage(message, platformConfig);
            return success("测试消息已发送到飞书单发用户");
        } catch (Exception e) {
            logger.error("飞书单发测试消息发送失败", e);
            return error("测试消息发送失败: " + e.getMessage());
        }
    }
}
