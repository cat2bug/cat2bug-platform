package com.cat2bug.web.controller.system;

import com.cat2bug.common.annotation.Log;
import com.cat2bug.common.core.controller.BaseController;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.core.page.TableDataInfo;
import com.cat2bug.common.enums.BusinessType;
import com.cat2bug.common.utils.poi.ExcelUtil;
import com.cat2bug.system.domain.SysDocument;
import com.cat2bug.system.service.ISysDocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 文档Controller
 * 
 * @author yuzhantao
 * @date 2024-06-17
 */
@RestController
@RequestMapping("/system/document")
public class SysDocumentController extends BaseController
{
    @Autowired
    private ISysDocumentService sysDocumentService;

    /**
     * 查询文档列表
     */
    @PreAuthorize("@ss.hasPermi('system:document:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysDocument sysDocument)
    {
        startPage();
        List<SysDocument> list = sysDocumentService.selectSysDocumentList(sysDocument);
        return getDataTable(list);
    }

    /**
     * 导出文档列表
     */
    @PreAuthorize("@ss.hasPermi('system:document:export')")
    @Log(title = "文档", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysDocument sysDocument)
    {
        List<SysDocument> list = sysDocumentService.selectSysDocumentList(sysDocument);
        ExcelUtil<SysDocument> util = new ExcelUtil<SysDocument>(SysDocument.class);
        util.exportExcel(response, list, "文档数据");
    }

    /**
     * 获取文档详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:document:query')")
    @GetMapping(value = "/{docId}")
    public AjaxResult getInfo(@PathVariable("docId") Long docId)
    {
        return success(sysDocumentService.selectSysDocumentByDocId(docId));
    }

    /**
     * 新增文档
     */
    @PreAuthorize("@ss.hasPermi('system:document:add')")
    @Log(title = "文档", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SysDocument sysDocument)
    {
        return toAjax(sysDocumentService.insertSysDocument(sysDocument));
    }

    /**
     * 修改文档
     */
    @PreAuthorize("@ss.hasPermi('system:document:edit')")
    @Log(title = "文档", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SysDocument sysDocument)
    {
        return toAjax(sysDocumentService.updateSysDocument(sysDocument));
    }

    /**
     * 删除文档
     */
    @PreAuthorize("@ss.hasPermi('system:document:remove')")
    @Log(title = "文档", businessType = BusinessType.DELETE)
	@DeleteMapping("/{docIds}")
    public AjaxResult remove(@PathVariable Long[] docIds)
    {
        return toAjax(sysDocumentService.deleteSysDocumentByDocIds(docIds));
    }
}
