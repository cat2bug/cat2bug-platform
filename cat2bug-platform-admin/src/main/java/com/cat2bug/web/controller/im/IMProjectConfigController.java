package com.cat2bug.web.controller.im;

import com.cat2bug.common.annotation.Log;
import com.cat2bug.common.core.controller.BaseController;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.core.page.TableDataInfo;
import com.cat2bug.common.enums.BusinessType;
import com.cat2bug.common.utils.poi.ExcelUtil;
import com.cat2bug.im.domain.EnterpriseWeChatProjectConfig;
import com.cat2bug.im.domain.IMProjectConfig;
import com.cat2bug.im.service.IIMProjectConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 项目通知配置Controller
 * 
 * @author yuzhantao
 * @date 2024-07-27
 */
@RestController
@RequestMapping("/im/project/wechat/config")
public class IMProjectConfigController extends BaseController
{
    @Autowired
    private IIMProjectConfigService imProjectConfigService;

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
}
