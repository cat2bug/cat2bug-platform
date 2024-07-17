package com.cat2bug.web.controller.im;

import com.cat2bug.common.annotation.Log;
import com.cat2bug.common.core.controller.BaseController;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.enums.BusinessType;
import com.cat2bug.im.domain.IMUserConfig;
import com.cat2bug.im.service.IIMUserConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 用户消息配置Controller
 * 
 * @author yuzhantao
 * @date 2024-07-18
 */
@RestController
@RequestMapping("/im/config")
public class IMUserConfigController extends BaseController
{
    @Autowired
    private IIMUserConfigService imUserConfigService;

    /**
     * 获取用户消息配置详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:notice:list')")
    @GetMapping()
    public AjaxResult getInfo(@RequestParam("projectId") Long projectId, @RequestParam("memberId") Long memberId)
    {
        return success(imUserConfigService.selectImUserConfigByProjectAndMember(projectId, memberId));
    }

    /**
     * 新增用户消息配置
     */
    @PreAuthorize("@ss.hasPermi('system:notice:list')")
    @Log(title = "用户消息配置", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult save(@RequestBody IMUserConfig imUserConfig)
    {
        return toAjax(imUserConfigService.saveImUserConfig(imUserConfig));
    }
}
