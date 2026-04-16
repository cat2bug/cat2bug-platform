package com.cat2bug.web.controller.im;

import com.cat2bug.common.annotation.Log;
import com.cat2bug.common.core.controller.BaseController;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.enums.BusinessType;
import com.cat2bug.im.domain.FeishuProjectConfig;
import com.cat2bug.im.domain.IMProjectConfig;
import com.cat2bug.im.service.IIMProjectConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
}
