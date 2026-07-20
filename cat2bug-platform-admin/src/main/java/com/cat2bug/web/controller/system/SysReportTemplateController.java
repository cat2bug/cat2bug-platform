package com.cat2bug.web.controller.system;

import com.cat2bug.common.annotation.Log;
import com.cat2bug.common.core.controller.BaseController;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.core.page.TableDataInfo;
import com.cat2bug.common.enums.BusinessType;
import com.cat2bug.system.domain.SysReportTemplate;
import com.cat2bug.system.service.ISysReportTemplateService;
import com.cat2bug.web.domain.report.ReportTemplatePickerResponse;
import com.cat2bug.web.excel.ExcelHttpSupport;
import com.cat2bug.web.service.excel.SystemExcelExportService;
import com.cat2bug.web.service.report.ReportPresetTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @Autowired
    private SystemExcelExportService systemExcelExportService;

    @Autowired
    private ReportPresetTemplateService reportPresetTemplateService;

    /**
     * 查询报告模版列表（仅 DB 自定义；管理页使用）
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
     * 分组选择器：自定义（DB）+ classpath 预制
     */
    @PreAuthorize("@ss.hasPermi('system:report:list')")
    @GetMapping("/picker")
    public AjaxResult picker(
            @RequestParam(value = "projectId", required = false) Long projectId,
            @RequestParam(value = "pageNum", required = false) Integer pageNum,
            @RequestParam(value = "pageSize", required = false) Integer pageSize)
    {
        SysReportTemplate query = new SysReportTemplate();
        query.setProjectId(projectId);
        if (pageNum != null && pageSize != null)
        {
            startPage();
        }
        List<SysReportTemplate> customs = sysReportTemplateService.selectSysReportTemplateList(query);
        ReportTemplatePickerResponse response = reportPresetTemplateService.buildPicker(customs);
        if (pageNum != null && pageSize != null)
        {
            TableDataInfo page = getDataTable(customs);
            if (response.getGroups() != null && !response.getGroups().isEmpty())
            {
                response.getGroups().get(0).setTotal(page.getTotal());
            }
        }
        return success(response);
    }

    /**
     * 导出报告模版列表
     */
    @PreAuthorize("@ss.hasPermi('system:report:export')")
    @Log(title = "报告模版", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysReportTemplate sysReportTemplate) throws IOException
    {
        ExcelHttpSupport.write(response, systemExcelExportService.exportReportTemplates(sysReportTemplate), "报告模版数据.xlsx");
    }

    /**
     * 预制正文（29119-3 单段 key）
     */
    @PreAuthorize("@ss.hasPermi('system:report:list')")
    @GetMapping("/preset/{group}/{key}/content")
    public AjaxResult presetContent(
            @PathVariable("group") String group,
            @PathVariable("key") String key)
    {
        Map<String, String> body = new HashMap<>(2);
        body.put("content", reportPresetTemplateService.readMarkdownContent(group, key, null));
        return success(body);
    }

    /**
     * 预制正文（9001 manuals|records）
     */
    @PreAuthorize("@ss.hasPermi('system:report:list')")
    @GetMapping("/preset/{group}/{folder}/{key}/content")
    public AjaxResult presetContentWithFolder(
            @PathVariable("group") String group,
            @PathVariable("folder") String folder,
            @PathVariable("key") String key)
    {
        Map<String, String> body = new HashMap<>(2);
        body.put("content", reportPresetTemplateService.readMarkdownContent(group, folder, key));
        return success(body);
    }

    /**
     * 预制预览图（29119-3）。匿名可读：卡片用 &lt;img&gt; 加载，不会带 Authorization。
     */
    @GetMapping("/preset/{group}/{key}/preview")
    public ResponseEntity<byte[]> presetPreview(
            @PathVariable("group") String group,
            @PathVariable("key") String key)
    {
        byte[] png = reportPresetTemplateService.readPreviewPng(group, key, null);
        return ResponseEntity.ok()
                // 预览会随同步脚本重渲；禁止长期缓存，避免黑底旧图残留
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, max-age=0, must-revalidate")
                .contentType(MediaType.IMAGE_PNG)
                .body(png);
    }

    /**
     * 预制预览图（9001）。匿名可读：同上。
     */
    @GetMapping("/preset/{group}/{folder}/{key}/preview")
    public ResponseEntity<byte[]> presetPreviewWithFolder(
            @PathVariable("group") String group,
            @PathVariable("folder") String folder,
            @PathVariable("key") String key)
    {
        byte[] png = reportPresetTemplateService.readPreviewPng(group, folder, key);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, max-age=0, must-revalidate")
                .contentType(MediaType.IMAGE_PNG)
                .body(png);
    }

    /**
     * 复制预制到项目自定义模版（29119-3）
     */
    @PreAuthorize("@ss.hasPermi('system:report:add')")
    @Log(title = "报告模版", businessType = BusinessType.INSERT)
    @PostMapping("/preset/{group}/{key}/copy")
    public AjaxResult copyPreset(
            @PathVariable("group") String group,
            @PathVariable("key") String key,
            @RequestParam("projectId") Long projectId)
    {
        SysReportTemplate copy = reportPresetTemplateService.buildCustomCopy(projectId, group, key, null);
        int ok = sysReportTemplateService.insertSysReportTemplate(copy);
        return success(ok > 0 ? copy : null);
    }

    /**
     * 复制预制到项目自定义模版（9001）
     */
    @PreAuthorize("@ss.hasPermi('system:report:add')")
    @Log(title = "报告模版", businessType = BusinessType.INSERT)
    @PostMapping("/preset/{group}/{folder}/{key}/copy")
    public AjaxResult copyPresetWithFolder(
            @PathVariable("group") String group,
            @PathVariable("folder") String folder,
            @PathVariable("key") String key,
            @RequestParam("projectId") Long projectId)
    {
        SysReportTemplate copy = reportPresetTemplateService.buildCustomCopy(projectId, group, folder, key);
        int ok = sysReportTemplateService.insertSysReportTemplate(copy);
        return success(ok > 0 ? copy : null);
    }

    /**
     * 获取报告模版详细信息（仅自定义 DB id；预制无 DB 行）
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
        return success(isSuccess > 0 ? sysReportTemplate : null);
    }

    /**
     * 修改报告模版（仅自定义；预制无 templateId）
     */
    @PreAuthorize("@ss.hasPermi('system:report:add')")
    @Log(title = "报告模版", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SysReportTemplate sysReportTemplate)
    {
        // Presets live on classpath and have no DB id — update only applies to custom rows.
        int isSuccess = sysReportTemplateService.updateSysReportTemplate(sysReportTemplate);
        return success(isSuccess > 0 ? sysReportTemplate : null);
    }

    /**
     * 删除报告模版（仅自定义）
     */
    @PreAuthorize("@ss.hasPermi('system:report:add')")
    @Log(title = "报告模版", businessType = BusinessType.DELETE)
    @DeleteMapping("/{templateIds}")
    public AjaxResult remove(@PathVariable Long[] templateIds)
    {
        return toAjax(sysReportTemplateService.deleteSysReportTemplateByTemplateIds(templateIds));
    }
}
