package com.cat2bug.web.controller.system;

import com.cat2bug.common.annotation.Log;
import com.cat2bug.common.core.controller.BaseController;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.core.page.TableDataInfo;
import com.cat2bug.common.enums.BusinessType;
import com.cat2bug.common.utils.poi.ExcelUtil;
import com.cat2bug.system.domain.SysDefect;
import com.cat2bug.system.service.ISysDefectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 缺陷Controller
 * 
 * @author yuzhantao
 * @date 2023-11-23
 */
@RestController
@RequestMapping("/system/defect")
public class SysDefectController extends BaseController
{
    @Autowired
    private ISysDefectService sysDefectService;

    /**
     * 查询缺陷列表
     */
    @PreAuthorize("@ss.hasPermi('system:defect:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysDefect sysDefect)
    {
        startPage();
        List<SysDefect> list = sysDefectService.selectSysDefectList(sysDefect);
        return getDataTable(list);
    }

    /**
     * 导出缺陷列表
     */
    @PreAuthorize("@ss.hasPermi('system:defect:export')")
    @Log(title = "缺陷", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysDefect sysDefect)
    {
        List<SysDefect> list = sysDefectService.selectSysDefectList(sysDefect);
        ExcelUtil<SysDefect> util = new ExcelUtil<SysDefect>(SysDefect.class);
        util.exportExcel(response, list, "缺陷数据");
    }

    /**
     * 获取缺陷详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:defect:query')")
    @GetMapping(value = "/{defectId}")
    public AjaxResult getInfo(@PathVariable("defectId") Long defectId)
    {
        return success(sysDefectService.selectSysDefectByDefectId(defectId));
    }

    /**
     * 新增缺陷
     */
    @PreAuthorize("@ss.hasPermi('system:defect:add')")
    @Log(title = "缺陷", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SysDefect sysDefect)
    {
        return toAjax(sysDefectService.insertSysDefect(sysDefect));
    }

    /**
     * 修改缺陷
     */
    @PreAuthorize("@ss.hasPermi('system:defect:edit')")
    @Log(title = "缺陷", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SysDefect sysDefect)
    {
        return toAjax(sysDefectService.updateSysDefect(sysDefect));
    }

    /**
     * 删除缺陷
     */
    @PreAuthorize("@ss.hasPermi('system:defect:remove')")
    @Log(title = "缺陷", businessType = BusinessType.DELETE)
	@DeleteMapping("/{defectIds}")
    public AjaxResult remove(@PathVariable Long[] defectIds)
    {
        return toAjax(sysDefectService.deleteSysDefectByDefectIds(defectIds));
    }
}
