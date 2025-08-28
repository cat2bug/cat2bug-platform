package com.cat2bug.web.controller.system;

import com.cat2bug.common.annotation.Log;
import com.cat2bug.common.core.controller.BaseController;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.core.page.TableDataInfo;
import com.cat2bug.common.enums.BusinessType;
import com.cat2bug.common.utils.poi.ExcelUtil;
import com.cat2bug.system.domain.SysProjectDefectTabs;
import com.cat2bug.system.service.ISysProjectDefectTabsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 项目缺陷页签配置Controller
 * 
 * @author yuzhantao
 * @date 2024-04-06
 */
@RestController
@RequestMapping("/system/tabs")
public class SysProjectDefectTabsController extends BaseController
{
    @Autowired
    private ISysProjectDefectTabsService sysProjectDefectTabsService;

    /**
     * 查询项目缺陷页签配置列表
     */
    @PreAuthorize("@ss.hasPermi('system:defect:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysProjectDefectTabs sysProjectDefectTabs)
    {
        startPage();
        List<SysProjectDefectTabs> list = sysProjectDefectTabsService.selectSysProjectDefectTabsList(sysProjectDefectTabs);
        return getDataTable(list);
    }

    /**
     * 获取项目缺陷页签配置详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:defect:list')")
    @GetMapping(value = "/{tabId}")
    public AjaxResult getInfo(@PathVariable("tabId") Long tabId)
    {
        return success(sysProjectDefectTabsService.selectSysProjectDefectTabsByTabId(tabId));
    }

    /**
     * 新增项目缺陷页签配置
     */
    @PreAuthorize("@ss.hasPermi('system:defect:list')")
    @Log(title = "项目缺陷页签配置", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SysProjectDefectTabs sysProjectDefectTabs)
    {
        return AjaxResult.success(sysProjectDefectTabsService.insertSysProjectDefectTabs(sysProjectDefectTabs));
    }

    /**
     * 修改项目缺陷页签配置
     */
    @PreAuthorize("@ss.hasPermi('system:defect:list')")
    @Log(title = "项目缺陷页签配置", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SysProjectDefectTabs sysProjectDefectTabs)
    {
        return toAjax(sysProjectDefectTabsService.updateSysProjectDefectTabs(sysProjectDefectTabs));
    }

    @PreAuthorize("@ss.hasPermi('system:defect:list')")
    @Log(title = "项目缺陷页签配置", businessType = BusinessType.UPDATE)
    @PutMapping("/sort")
    public AjaxResult updateSort(@RequestBody List<SysProjectDefectTabs> list)
    {
        return toAjax(sysProjectDefectTabsService.updateSort(list));
    }

    /**
     * 删除项目缺陷页签配置
     */
    @PreAuthorize("@ss.hasPermi('system:defect:list')")
    @Log(title = "项目缺陷页签配置", businessType = BusinessType.DELETE)
	@DeleteMapping("/{tabIds}")
    public AjaxResult remove(@PathVariable Long[] tabIds)
    {
        return toAjax(sysProjectDefectTabsService.deleteSysProjectDefectTabsByTabIds(tabIds));
    }
}
