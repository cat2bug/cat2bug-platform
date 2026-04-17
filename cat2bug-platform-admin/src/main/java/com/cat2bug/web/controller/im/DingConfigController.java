package com.cat2bug.web.controller.im;

import com.alibaba.fastjson.JSON;
import com.cat2bug.common.annotation.Log;
import com.cat2bug.common.core.controller.BaseController;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.enums.BusinessType;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.im.domain.DingMessage;
import com.cat2bug.im.domain.DingProjectConfig;
import com.cat2bug.im.domain.IMDingPlatformConfig;
import com.cat2bug.im.domain.IMProjectConfig;
import com.cat2bug.im.service.IIMProjectConfigService;
import com.cat2bug.im.service.impl.DingMessageServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Ding在项目中的配置Controller
 *
 * @author yuzhantao
 * @date 2024-07-27
 */
@RestController
@RequestMapping("/im/project/ding/config")
public class DingConfigController extends BaseController
{
    @Autowired
    private IIMProjectConfigService imProjectConfigService;

    @Autowired
    private DingMessageServiceImpl dingMessageService;

    /**
     * 获取钉钉配置详细信息
     */
    @PreAuthorize("@ss.hasPermi('ding:query')")
    @GetMapping
    public AjaxResult getInfo(@RequestParam("projectId") Long projectId)
    {
        return success(imProjectConfigService.selectImProjectConfigByProjectIdAndSystemCode(
                projectId,
                DingProjectConfig.SYSTEM_CODE));
    }

    /**
     * 保存钉钉配置
     */
    @PreAuthorize("@ss.hasPermi('ding:save')")
    @Log(title = "项目通知配置", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult save(@RequestBody IMProjectConfig imProjectConfig)
    {
        IMProjectConfig config = imProjectConfigService.selectImProjectConfigByProjectIdAndSystemCode(
                imProjectConfig.getProjectId(),
                DingProjectConfig.SYSTEM_CODE);

        imProjectConfig.setSystemCode(DingProjectConfig.SYSTEM_CODE);
        if(config==null) {
            return toAjax(imProjectConfigService.insertImProjectConfig(imProjectConfig));
        } else {
            imProjectConfig.setConfigId(config.getConfigId());
            return toAjax(imProjectConfigService.updateImProjectConfig(imProjectConfig));
        }
    }

    /**
     * 群发测试钉钉通知
     */
    @PreAuthorize("@ss.hasPermi('ding:query')")
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

            String testMessage = "这是来自 Cat2Bug 的钉钉测试消息";
            if (StringUtils.isNotBlank(key)) {
                testMessage = testMessage + "，关键词：" + key;
            }

            DingMessage message = new DingMessage(testMessage);
            message.setMsgtype("text");
            message.setProjectId(projectId);
            message.setReceiveMemberId(memberId);
            message.setWebHook(hook);

            IMDingPlatformConfig platformConfig = new IMDingPlatformConfig();
            platformConfig.setHook(hook);
            platformConfig.setKey(key);
            platformConfig.setSecret(secret);

            dingMessageService.sendNoticeMessage(message, platformConfig);
            return success("测试消息已发送到钉钉群机器人");
        } catch (Exception e) {
            logger.error("钉钉群发测试消息发送失败", e);
            return error("测试消息发送失败: " + e.getMessage());
        }
    }

    /**
     * 单发测试钉钉通知
     */
    @PreAuthorize("@ss.hasPermi('ding:query')")
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

            IMProjectConfig projectConfig = imProjectConfigService.selectImProjectConfigByProjectIdAndSystemCode(projectId, DingProjectConfig.SYSTEM_CODE);
            if (projectConfig == null || StringUtils.isBlank(projectConfig.getConfigParams())) {
                return error("请先在项目配置中设置钉钉企业应用配置");
            }

            DingProjectConfig dingProjectConfig = JSON.parseObject(projectConfig.getConfigParams(), DingProjectConfig.class);

            DingMessage message = new DingMessage("这是来自 Cat2Bug 的钉钉测试消息");
            message.setMsgtype("text");
            message.setProjectId(projectId);
            message.setReceiveMemberId(memberId);
            message.setAppKey(dingProjectConfig.getAppKey());
            message.setAppSecret(dingProjectConfig.getAppSecret());
            message.setRobotCode(dingProjectConfig.getRobotCode());

            // 设置消息参数
            com.cat2bug.im.domain.DingSampleActionCardParams msgParams = new com.cat2bug.im.domain.DingSampleActionCardParams();
            msgParams.setTitle("Cat2Bug 测试消息");
            msgParams.setText("这是来自 Cat2Bug 的钉钉测试消息");
            msgParams.setSingleTitle("确认");
            msgParams.setSingleURL("https://www.cat2bug.com");
            message.setMsgParam(JSON.toJSONString(msgParams));

            IMDingPlatformConfig platformConfig = new IMDingPlatformConfig();
            platformConfig.setMobile(mobile);

            dingMessageService.sendNoticeMessage(message, platformConfig);
            return success("测试消息已发送到钉钉单发用户");
        } catch (Exception e) {
            logger.error("钉钉单发测试消息发送失败", e);
            return error("测试消息发送失败: " + e.getMessage());
        }
    }
}
