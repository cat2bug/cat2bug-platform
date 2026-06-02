package com.cat2bug.web.controller.system;

import com.cat2bug.common.annotation.Log;
import com.cat2bug.common.core.controller.BaseController;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.enums.BusinessType;
import com.cat2bug.system.domain.SysProjectDefectField;
import com.cat2bug.system.domain.SysProjectDefectFieldManageItem;
import com.cat2bug.system.service.ISysProjectDefectFieldService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 项目缺陷自定义字段定义 Controller
 */
@RestController
@RequestMapping("/system/project/{projectId}/defect-fields")
public class SysProjectDefectFieldController extends BaseController {

    @Autowired
    private ISysProjectDefectFieldService defectFieldService;

    @PreAuthorize("@ss.hasPermi('system:project:defect-field:list')")
    @GetMapping
    public AjaxResult list(@PathVariable Long projectId) {
        List<SysProjectDefectFieldManageItem> list = defectFieldService.selectManageListByProjectId(projectId);
        return success(list);
    }

    @PreAuthorize("@ss.hasPermi('system:project:defect-field:edit')")
    @Log(title = "缺陷内置字段布局", businessType = BusinessType.UPDATE)
    @PutMapping("/builtin-layout")
    public AjaxResult updateBuiltinLayout(
            @PathVariable Long projectId,
            @RequestBody List<SysProjectDefectFieldManageItem> layout) {
        return toAjax(defectFieldService.updateBuiltinFieldLayout(projectId, layout));
    }

    @PreAuthorize("@ss.hasPermi('system:defect:list')")
    @GetMapping("/enabled")
    public AjaxResult enabledList(@PathVariable Long projectId) {
        return success(defectFieldService.selectEnabledListByProjectId(projectId));
    }

    @PreAuthorize("@ss.hasPermi('system:defect:list')")
    @GetMapping("/column-layout")
    public AjaxResult columnLayout(@PathVariable Long projectId) {
        return success(defectFieldService.selectColumnLayoutByProjectId(projectId));
    }

    @PreAuthorize("@ss.hasPermi('system:project:defect-field:add')")
    @Log(title = "缺陷自定义字段", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@PathVariable Long projectId, @RequestBody SysProjectDefectField field) {
        field.setProjectId(projectId);
        return success(defectFieldService.insert(field));
    }

    @PreAuthorize("@ss.hasPermi('system:project:defect-field:edit')")
    @Log(title = "缺陷自定义字段", businessType = BusinessType.UPDATE)
    @PutMapping("/{fieldId}")
    public AjaxResult edit(
            @PathVariable Long projectId,
            @PathVariable Long fieldId,
            @RequestBody SysProjectDefectField field) {
        field.setProjectId(projectId);
        field.setFieldId(fieldId);
        return toAjax(defectFieldService.update(field));
    }

    @PreAuthorize("@ss.hasPermi('system:project:defect-field:remove')")
    @Log(title = "缺陷自定义字段", businessType = BusinessType.DELETE)
    @DeleteMapping("/{fieldId}")
    public AjaxResult remove(@PathVariable Long projectId, @PathVariable Long fieldId) {
        return toAjax(defectFieldService.softDelete(projectId, fieldId));
    }
}
