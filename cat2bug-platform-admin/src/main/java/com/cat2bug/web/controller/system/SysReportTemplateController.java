package com.cat2bug.web.controller.system;

import com.cat2bug.common.annotation.Log;
import com.cat2bug.common.core.controller.BaseController;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.core.page.TableDataInfo;
import com.cat2bug.common.enums.BusinessType;
import com.cat2bug.common.utils.poi.ExcelUtil;
import com.cat2bug.system.domain.SysReportTemplate;
import com.cat2bug.system.service.ISysReportTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 报告模版Controller
 * 
 * @author yuzhantao
 * @date 2024-04-29
 */
@RestController
@RequestMapping("/system/report/template")
public class SysReportTemplateController extends BaseController
{
    @Autowired
    private ISysReportTemplateService sysReportTemplateService;

    /**
     * 查询报告模版列表
     */
    @PreAuthorize("@ss.hasPermi('system:report:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysReportTemplate sysReportTemplate)
    {
        startPage();
        List<SysReportTemplate> list = sysReportTemplateService.selectSysReportTemplateList(sysReportTemplate);
        return getDataTable(list);
    }

    /**
     * 导出报告模版列表
     */
    @PreAuthorize("@ss.hasPermi('system:report:export')")
    @Log(title = "报告模版", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysReportTemplate sysReportTemplate)
    {
        List<SysReportTemplate> list = sysReportTemplateService.selectSysReportTemplateList(sysReportTemplate);
        ExcelUtil<SysReportTemplate> util = new ExcelUtil<SysReportTemplate>(SysReportTemplate.class);
        util.exportExcel(response, list, "报告模版数据");
    }

    /**
     * 获取报告模版详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:report:list')")
    @GetMapping(value = "/{templateId}")
    public AjaxResult getInfo(@PathVariable("templateId") Long templateId)
    {
        return success(sysReportTemplateService.selectSysReportTemplateByTemplateId(templateId));
    }

    /**
     * 新增报告模版
     */
    @PreAuthorize("@ss.hasPermi('system:report:add')")
    @Log(title = "报告模版", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SysReportTemplate sysReportTemplate)
    {
        int isSuccess = sysReportTemplateService.insertSysReportTemplate(sysReportTemplate);
        return success(isSuccess>0?sysReportTemplate:null);
    }

    /**
     * 修改报告模版
     */
    @PreAuthorize("@ss.hasPermi('system:report:add')")
    @Log(title = "报告模版", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SysReportTemplate sysReportTemplate)
    {
        int isSuccess = sysReportTemplateService.updateSysReportTemplate(sysReportTemplate);
        return success(isSuccess>0?sysReportTemplate:null);
    }

    /**
     * 删除报告模版
     */
    @PreAuthorize("@ss.hasPermi('system:report:add')")
    @Log(title = "报告模版", businessType = BusinessType.DELETE)
	@DeleteMapping("/{templateIds}")
    public AjaxResult remove(@PathVariable Long[] templateIds)
    {
        return toAjax(sysReportTemplateService.deleteSysReportTemplateByTemplateIds(templateIds));
    }
}
