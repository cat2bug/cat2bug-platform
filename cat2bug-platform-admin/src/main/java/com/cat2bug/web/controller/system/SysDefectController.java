package com.cat2bug.web.controller.system;

import com.cat2bug.common.annotation.Log;
import com.cat2bug.common.core.controller.BaseController;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.common.core.page.TableDataInfo;
import com.cat2bug.common.enums.BusinessType;
import com.cat2bug.common.utils.poi.ExcelUtil;
import com.cat2bug.common.core.domain.entity.SysDefect;
import com.cat2bug.system.domain.SysDefectLog;
import com.cat2bug.system.domain.SysProjectDefectTabs;
import com.cat2bug.system.domain.SysUserConfig;
import com.cat2bug.system.service.IMemberFocusService;
import com.cat2bug.system.service.ISysDefectService;
import com.cat2bug.system.service.ISysProjectDefectTabsService;
import com.cat2bug.system.service.ISysUserConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private final static String MODULE_NAME = "defect";

    @Autowired
    private ISysDefectService sysDefectService;
    @Autowired
    private IMemberFocusService memberFocusService;
    @Autowired
    private ISysProjectDefectTabsService sysProjectDefectTabsService;
    @Autowired
    private ISysUserConfigService sysUserConfigService;
    /**
     * 查询缺陷配置
     */
    @PreAuthorize("@ss.hasPermi('system:defect:list')")
    @GetMapping("/config")
    public AjaxResult config()
    {
        SysUserConfig userConfig = sysUserConfigService.selectSysUserConfigByUserId(getUserId());
        Map<String,Object> ret = new HashMap<>();
        ret.put("types",sysDefectService.getDefectTypeList());
        ret.put("states",sysDefectService.getDefectStateList());

        SysProjectDefectTabs pdt =new SysProjectDefectTabs();
        pdt.setProjectId(userConfig.getCurrentProjectId());
        pdt.setUserId(getUserId());
        ret.put("tabs",sysProjectDefectTabsService.selectSysProjectDefectTabsList(pdt));
        return success(ret);
    }

    /**
     * 查询缺陷列表
     */
    @PreAuthorize("@ss.hasPermi('system:defect:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysDefect sysDefect)
    {
        startPage();
        List<SysDefect> list = sysDefectService.selectSysDefectList(sysDefect);
        TableDataInfo tableDataInfo = getDataTable(list);
        List<SysDefect> newList = new ArrayList<>(list);
        newList.forEach(l->{
            if(l.getHandleByList()!=null){
                l.setHandleByList(l.getHandleByList().stream().filter(h->h.getUserId()>0).collect(Collectors.toList()));
            }
            List<SysUser> focusList = memberFocusService.getFocusMemberList(MODULE_NAME,l.getDefectId());
            l.setFocusList(focusList);
        });
        tableDataInfo.setRows(newList);
        return tableDataInfo;
    }

    /**
     * 关闭前端编辑窗口
     */
    @Log(title = "驳回缺陷", businessType = BusinessType.INSERT)
    @PostMapping("/{defectId}/close-edit-window")
    public AjaxResult closeEditWindows(@PathVariable("defectId") Long defectId)
    {
        memberFocusService.removeFocus(getUserId());
        return success();
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
        SysDefect sysDefect = sysDefectService.selectSysDefectByDefectId(defectId,getUserId());
        if(sysDefect!=null) {
            memberFocusService.setFocus(MODULE_NAME, defectId, this.getLoginUser().getUser());
            List<SysUser> focusList = memberFocusService.getFocusMemberList(MODULE_NAME,defectId);
            sysDefect.setFocusList(focusList);
        }
        return success(sysDefect);
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
     * 指派
     */
    @PreAuthorize("@ss.hasPermi('system:defect:assign')")
    @Log(title = "指派缺陷", businessType = BusinessType.INSERT)
    @PostMapping("/{defectId}/assign")
    public AjaxResult assign(@RequestBody SysDefectLog sysDefectlog)
    {
        return success(sysDefectService.assign(sysDefectlog));
    }

    /**
     * 驳回
     */
    @PreAuthorize("@ss.hasPermi('system:defect:reject')")
    @Log(title = "驳回缺陷", businessType = BusinessType.INSERT)
    @PostMapping("/{defectId}/reject")
    public AjaxResult reject(@RequestBody SysDefectLog sysDefectlog)
    {
        return success(sysDefectService.reject(sysDefectlog));
    }

    /**
     * 修复
     */
    @PreAuthorize("@ss.hasPermi('system:defect:repair')")
    @Log(title = "修复缺陷", businessType = BusinessType.INSERT)
    @PostMapping("/{defectId}/repair")
    public AjaxResult repair(@RequestBody SysDefectLog sysDefectlog)
    {
        return success(sysDefectService.repair(sysDefectlog));
    }

    /**
     * 通过
     */
    @PreAuthorize("@ss.hasPermi('system:defect:pass')")
    @Log(title = "通过缺陷", businessType = BusinessType.INSERT)
    @PostMapping("/{defectId}/pass")
    public AjaxResult pass(@RequestBody SysDefectLog sysDefectlog)
    {
        return success(sysDefectService.pass(sysDefectlog));
    }

    /**
     * 关闭
     */
    @PreAuthorize("@ss.hasPermi('system:defect:close')")
    @Log(title = "关闭缺陷", businessType = BusinessType.INSERT)
    @PostMapping("/{defectId}/close")
    public AjaxResult close(@RequestBody SysDefectLog sysDefectlog)
    {
        return success(sysDefectService.close(sysDefectlog));
    }

    /**
     * 启动
     */
    @PreAuthorize("@ss.hasPermi('system:defect:open')")
    @Log(title = "关闭缺陷", businessType = BusinessType.INSERT)
    @PostMapping("/{defectId}/open")
    public AjaxResult open(@RequestBody SysDefectLog sysDefectlog)
    {
        return success(sysDefectService.open(sysDefectlog));
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
