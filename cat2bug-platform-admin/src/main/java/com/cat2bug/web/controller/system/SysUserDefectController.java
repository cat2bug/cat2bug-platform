package com.cat2bug.web.controller.system;

import com.cat2bug.common.annotation.Log;
import com.cat2bug.common.core.controller.BaseController;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.core.page.TableDataInfo;
import com.cat2bug.common.enums.BusinessType;
import com.cat2bug.common.utils.poi.ExcelUtil;
import com.cat2bug.system.domain.SysUserDefect;
import com.cat2bug.system.service.ISysUserDefectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 用户缺陷Controller
 * 
 * @author yuzhantao
 * @date 2024-01-10
 */
@RestController
@RequestMapping("/system/defect")
public class SysUserDefectController extends BaseController
{
    @Autowired
    private ISysUserDefectService sysUserDefectService;

    /**
     * 修改用户缺陷
     */
    @PreAuthorize("@ss.hasPermi('system:defect:list') || @ss.hasPermi('system:defect:query')")
    @Log(title = "用户缺陷", businessType = BusinessType.UPDATE)
    @PutMapping("/{defectId}")
    public AjaxResult edit(@PathVariable Long defectId, @RequestBody SysUserDefect sysUserDefect)
    {
        sysUserDefect.setDefectId(defectId);
        return toAjax(sysUserDefectService.updateSysUserDefect(sysUserDefect));
    }
}
