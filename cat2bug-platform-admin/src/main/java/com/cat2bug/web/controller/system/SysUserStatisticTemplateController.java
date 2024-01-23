package com.cat2bug.web.controller.system;

import com.cat2bug.common.annotation.Log;
import com.cat2bug.common.core.controller.BaseController;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.core.page.TableDataInfo;
import com.cat2bug.common.enums.BusinessType;
import com.cat2bug.common.utils.poi.ExcelUtil;
import com.cat2bug.system.domain.SysUserStatisticTemplate;
import com.cat2bug.system.service.ISysUserStatisticTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 用户统计模版Controller
 * 
 * @author yuzhantao
 * @date 2024-01-24
 */
@RestController
@RequestMapping("/system/statistic")
public class SysUserStatisticTemplateController extends BaseController
{
    @Autowired
    private ISysUserStatisticTemplateService sysUserStatisticTemplateService;

    /**
     * 查询用户统计模版列表
     */
    @PreAuthorize("@ss.hasPermi('system:project:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysUserStatisticTemplate sysUserStatisticTemplate)
    {
        startPage();
        List<SysUserStatisticTemplate> list = sysUserStatisticTemplateService.selectSysUserStatisticTemplateList(sysUserStatisticTemplate);
        return getDataTable(list);
    }

    /**
     * 导出用户统计模版列表
     */
    @PreAuthorize("@ss.hasPermi('system:statistic:export')")
    @Log(title = "用户统计模版", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysUserStatisticTemplate sysUserStatisticTemplate)
    {
        List<SysUserStatisticTemplate> list = sysUserStatisticTemplateService.selectSysUserStatisticTemplateList(sysUserStatisticTemplate);
        ExcelUtil<SysUserStatisticTemplate> util = new ExcelUtil<SysUserStatisticTemplate>(SysUserStatisticTemplate.class);
        util.exportExcel(response, list, "用户统计模版数据");
    }

    /**
     * 获取用户统计模版详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:project:query')")
    @GetMapping(value = "/{statisticTemplateId}")
    public AjaxResult getInfo(@PathVariable("statisticTemplateId") Long statisticTemplateId)
    {
        return success(sysUserStatisticTemplateService.selectSysUserStatisticTemplateByStatisticTemplateId(statisticTemplateId));
    }

    /**
     * 新增用户统计模版
     */
    @PreAuthorize("@ss.hasPermi('system:project:list')")
    @Log(title = "用户统计模版", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SysUserStatisticTemplate sysUserStatisticTemplate)
    {
        return toAjax(sysUserStatisticTemplateService.insertSysUserStatisticTemplate(sysUserStatisticTemplate));
    }

    /**
     * 修改用户统计模版
     */
    @PreAuthorize("@ss.hasPermi('system:project:list')")
    @Log(title = "用户统计模版", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SysUserStatisticTemplate sysUserStatisticTemplate)
    {
        return toAjax(sysUserStatisticTemplateService.updateSysUserStatisticTemplate(sysUserStatisticTemplate));
    }

    /**
     * 删除用户统计模版
     */
    @PreAuthorize("@ss.hasPermi('system:project:list')")
    @Log(title = "用户统计模版", businessType = BusinessType.DELETE)
	@DeleteMapping("/{statisticTemplateIds}")
    public AjaxResult remove(@PathVariable Long[] statisticTemplateIds)
    {
        return toAjax(sysUserStatisticTemplateService.deleteSysUserStatisticTemplateByStatisticTemplateIds(statisticTemplateIds));
    }
}
