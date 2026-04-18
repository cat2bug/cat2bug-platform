package com.cat2bug.web.controller.im;

import com.cat2bug.common.annotation.Log;
import com.cat2bug.common.core.controller.BaseController;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.enums.BusinessType;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.im.domain.EnterpriseWeChatProjectConfig;
import com.cat2bug.im.domain.EnterpriseWeChatPlatformConfig;
import com.cat2bug.im.domain.IMProjectConfig;
import com.cat2bug.im.service.IIMProjectConfigService;
import com.cat2bug.im.service.impl.EnterpriseWeChatMessageServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 微信在项目中的配置Controller
 *
 * @author yuzhantao
 * @date 2024-07-27
 */
@RestController
@RequestMapping("/im/project/wechat/config")
public class WeChatConfigController extends BaseController
{
    @Autowired
    private IIMProjectConfigService imProjectConfigService;

    @Autowired
    private EnterpriseWeChatMessageServiceImpl enterpriseWeChatMessageService;

    /**
     * 获取企业微信配置详细信息
     */
    @PreAuthorize("@ss.hasPermi('wechat:query')")
    @GetMapping
    public AjaxResult getInfo(@RequestParam("projectId") Long projectId)
    {
        return success(imProjectConfigService.selectImProjectConfigByProjectIdAndSystemCode(
                projectId,
                EnterpriseWeChatProjectConfig.SYSTEM_CODE));
    }

    /**
     * 保存企业微信配置
     */
    @PreAuthorize("@ss.hasPermi('wechat:save')")
    @Log(title = "项目通知配置", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult save(@RequestBody IMProjectConfig imProjectConfig)
    {
        IMProjectConfig config = imProjectConfigService.selectImProjectConfigByProjectIdAndSystemCode(
                imProjectConfig.getProjectId(),
                EnterpriseWeChatProjectConfig.SYSTEM_CODE);

        imProjectConfig.setSystemCode(EnterpriseWeChatProjectConfig.SYSTEM_CODE);
        if(config==null) {
            return toAjax(imProjectConfigService.insertImProjectConfig(imProjectConfig));
        } else {
            imProjectConfig.setConfigId(config.getConfigId());
            return toAjax(imProjectConfigService.updateImProjectConfig(imProjectConfig));
        }
    }

    /**
     * 单发测试企业微信通知
     */
    @PreAuthorize("@ss.hasPermi('wechat:query')")
    @PostMapping("/single-test")
    public AjaxResult singleTest(@RequestBody Map<String, Object> params) {
        try {
            Long projectId = Long.valueOf(params.get("projectId").toString());
            Long memberId = Long.valueOf(params.get("memberId").toString());
            @SuppressWarnings("unchecked")
            Map<String, String> config = (Map<String, String>) params.get("config");

            String mobile = config.get("mobile");
            if (StringUtils.isBlank(mobile)) {
                return error("请配置单发手机号");
            }

            IMProjectConfig projectConfig = imProjectConfigService.selectImProjectConfigByProjectIdAndSystemCode(projectId, EnterpriseWeChatProjectConfig.SYSTEM_CODE);
            if (projectConfig == null || StringUtils.isBlank(projectConfig.getConfigParams())) {
                return error("请先在项目配置中设置企业微信应用配置");
            }

            EnterpriseWeChatPlatformConfig platformConfig = new EnterpriseWeChatPlatformConfig();
            platformConfig.setConfigSwitch(true);
            platformConfig.setSingleSwitch(true);
            platformConfig.setGroupSwitch(false);
            platformConfig.setMobile(mobile);

            enterpriseWeChatMessageService.sendSingleTestMessage(projectId, memberId, platformConfig);
            return success("测试消息已发送到企业微信单发用户");
        } catch (Exception e) {
            logger.error("企业微信单发测试消息发送失败", e);
            return error("测试消息发送失败: " + e.getMessage());
        }
    }

    /**
     * 群发测试企业微信通知
     */
    @PreAuthorize("@ss.hasPermi('wechat:query')")
    @PostMapping("/group-test")
    public AjaxResult groupTest(@RequestBody Map<String, Object> params) {
        try {
            Long projectId = Long.valueOf(params.get("projectId").toString());
            Long memberId = Long.valueOf(params.get("memberId").toString());
            @SuppressWarnings("unchecked")
            Map<String, String> config = (Map<String, String>) params.get("config");

            String mobile = config.get("mobile");
            String hook = config.get("hook");
            if (StringUtils.isBlank(hook)) {
                return error("请配置群发 Webhook 地址");
            }

            EnterpriseWeChatPlatformConfig platformConfig = new EnterpriseWeChatPlatformConfig();
            platformConfig.setConfigSwitch(true);
            platformConfig.setSingleSwitch(false);
            platformConfig.setGroupSwitch(true);
            platformConfig.setMobile(mobile);
            platformConfig.setHook(hook);

            enterpriseWeChatMessageService.sendGroupTestMessage(projectId, memberId, platformConfig);
            return success("测试消息已发送到企业微信群机器人");
        } catch (Exception e) {
            logger.error("企业微信群发测试消息发送失败", e);
            return error("测试消息发送失败: " + e.getMessage());
        }
    }
}
