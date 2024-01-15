package com.cat2bug.web.controller.system;

import com.cat2bug.common.annotation.Log;
import com.cat2bug.common.core.controller.BaseController;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.core.page.TableDataInfo;
import com.cat2bug.common.enums.BusinessType;
import com.cat2bug.common.utils.poi.ExcelUtil;
import com.cat2bug.framework.web.service.SysPermissionService;
import com.cat2bug.system.domain.SysUserConfig;
import com.cat2bug.system.service.ISysUserConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 用户配置Controller
 * 
 * @author yuzhantao
 * @date 2023-11-16
 */
@RestController
@RequestMapping("/system/user-config")
public class SysUserConfigController extends BaseController
{
    @Autowired
    private SysPermissionService permissionService;

    @Autowired
    private ISysUserConfigService sysUserConfigService;

    /**
     * 获取用户配置详细信息
     */
    @GetMapping(value = "/{userConfigId}")
    public AjaxResult getInfo(@PathVariable("userConfigId") Long userConfigId)
    {
        return success(sysUserConfigService.selectSysUserConfigByUserConfigId(userConfigId));
    }

    /**
     * 新增用户配置
     */
    @Log(title = "用户配置", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SysUserConfig sysUserConfig)
    {
        int ret = sysUserConfigService.insertSysUserConfig(sysUserConfig);
        // 更新用户权限
        permissionService.updateRoleAndPermissionOfCurrentUser();
        return toAjax(ret);
    }

    /**
     * 修改用户配置
     */
    @Log(title = "用户配置", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SysUserConfig sysUserConfig)
    {
        int ret = sysUserConfigService.updateSysUserConfig(sysUserConfig);
        // 更新用户权限
        permissionService.updateRoleAndPermissionOfCurrentUser();
        return toAjax(ret);
    }

    /**
     * 删除用户配置
     */
    @Log(title = "用户配置", businessType = BusinessType.DELETE)
	@DeleteMapping("/{userConfigIds}")
    public AjaxResult remove(@PathVariable Long[] userConfigIds)
    {
        return toAjax(sysUserConfigService.deleteSysUserConfigByUserConfigIds(userConfigIds));
    }
}
