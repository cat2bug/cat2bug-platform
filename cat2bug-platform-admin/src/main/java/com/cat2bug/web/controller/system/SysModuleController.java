package com.cat2bug.web.controller.system;

import com.cat2bug.common.annotation.Log;
import com.cat2bug.common.core.controller.BaseController;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.enums.BusinessType;
import com.cat2bug.common.utils.MessageUtils;
import com.cat2bug.common.utils.poi.ExcelUtil;
import com.cat2bug.system.domain.SysModule;
import com.cat2bug.system.service.ISysModuleService;
import com.google.common.base.Preconditions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 模块Controller
 * 
 * @author yuzhantao
 * @date 2023-11-26
 */
@RestController
@RequestMapping("/system/module")
public class SysModuleController extends BaseController
{
    @Autowired
    private ISysModuleService sysModuleService;

    /**
     * 查询模块列表
     */
    @PreAuthorize("@ss.hasPermi('system:defect:list') || @ss.hasPermi('system:module:list')")
    @GetMapping("/list")
    public AjaxResult list(SysModule sysModule)
    {
        List<SysModule> list = sysModuleService.selectSysModuleList(sysModule);
        return success(list);
    }

    /**
     * 导出模块列表
     */
    @PreAuthorize("@ss.hasPermi('system:module:export')")
    @Log(title = "模块", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysModule sysModule)
    {
        List<SysModule> list = sysModuleService.selectSysModuleList(sysModule);
        ExcelUtil<SysModule> util = new ExcelUtil<SysModule>(SysModule.class);
        util.exportExcel(response, list, "模块数据");
    }

    /**
     * 获取模块详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:module:query')")
    @GetMapping(value = "/{moduleId}")
    public AjaxResult getInfo(@PathVariable("moduleId") Long moduleId)
    {
        return success(sysModuleService.selectSysModuleByModuleId(moduleId));
    }

    /**
     * 新增模块
     */
    @PreAuthorize("@ss.hasPermi('system:module:add')")
    @Log(title = "模块", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SysModule sysModule)
    {
        return success(sysModuleService.insertSysModule(sysModule));
    }

    /**
     * 修改模块
     */
    @PreAuthorize("@ss.hasPermi('system:module:edit')")
    @Log(title = "模块", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SysModule sysModule)
    {
        return toAjax(sysModuleService.updateSysModule(sysModule));
    }

    /**
     * 删除模块
     */
    @PreAuthorize("@ss.hasPermi('system:module:remove')")
    @Log(title = "模块", businessType = BusinessType.DELETE)
	@DeleteMapping("/{moduleIds}")
    public AjaxResult remove(@PathVariable Long[] moduleIds)
    {
        return toAjax(sysModuleService.deleteSysModuleByModuleIds(moduleIds));
    }
}
