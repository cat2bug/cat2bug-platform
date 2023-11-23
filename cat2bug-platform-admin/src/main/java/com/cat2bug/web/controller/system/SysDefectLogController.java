package com.cat2bug.web.controller.system;

import com.cat2bug.common.annotation.Log;
import com.cat2bug.common.core.controller.BaseController;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.core.page.TableDataInfo;
import com.cat2bug.common.enums.BusinessType;
import com.cat2bug.common.utils.poi.ExcelUtil;
import com.cat2bug.system.domain.SysDefectLog;
import com.cat2bug.system.service.ISysDefectLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 缺陷日志Controller
 * 
 * @author yuzhantao
 * @date 2023-11-23
 */
@RestController
@RequestMapping("/system/log")
public class SysDefectLogController extends BaseController
{
    @Autowired
    private ISysDefectLogService sysDefectLogService;

    /**
     * 查询缺陷日志列表
     */
    @PreAuthorize("@ss.hasPermi('system:log:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysDefectLog sysDefectLog)
    {
        startPage();
        List<SysDefectLog> list = sysDefectLogService.selectSysDefectLogList(sysDefectLog);
        return getDataTable(list);
    }

    /**
     * 导出缺陷日志列表
     */
    @PreAuthorize("@ss.hasPermi('system:log:export')")
    @Log(title = "缺陷日志", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysDefectLog sysDefectLog)
    {
        List<SysDefectLog> list = sysDefectLogService.selectSysDefectLogList(sysDefectLog);
        ExcelUtil<SysDefectLog> util = new ExcelUtil<SysDefectLog>(SysDefectLog.class);
        util.exportExcel(response, list, "缺陷日志数据");
    }

    /**
     * 获取缺陷日志详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:log:query')")
    @GetMapping(value = "/{defectLogId}")
    public AjaxResult getInfo(@PathVariable("defectLogId") Long defectLogId)
    {
        return success(sysDefectLogService.selectSysDefectLogByDefectLogId(defectLogId));
    }

    /**
     * 新增缺陷日志
     */
    @PreAuthorize("@ss.hasPermi('system:log:add')")
    @Log(title = "缺陷日志", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SysDefectLog sysDefectLog)
    {
        return toAjax(sysDefectLogService.insertSysDefectLog(sysDefectLog));
    }

    /**
     * 修改缺陷日志
     */
    @PreAuthorize("@ss.hasPermi('system:log:edit')")
    @Log(title = "缺陷日志", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SysDefectLog sysDefectLog)
    {
        return toAjax(sysDefectLogService.updateSysDefectLog(sysDefectLog));
    }

    /**
     * 删除缺陷日志
     */
    @PreAuthorize("@ss.hasPermi('system:log:remove')")
    @Log(title = "缺陷日志", businessType = BusinessType.DELETE)
	@DeleteMapping("/{defectLogIds}")
    public AjaxResult remove(@PathVariable Long[] defectLogIds)
    {
        return toAjax(sysDefectLogService.deleteSysDefectLogByDefectLogIds(defectLogIds));
    }
}
