package com.cat2bug.web.controller.im;

import com.cat2bug.common.annotation.Log;
import com.cat2bug.common.core.controller.BaseController;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.enums.BusinessType;
import com.cat2bug.im.domain.DingProjectConfig;
import com.cat2bug.im.domain.EnterpriseWeChatProjectConfig;
import com.cat2bug.im.domain.IMProjectConfig;
import com.cat2bug.im.service.IIMProjectConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
}
