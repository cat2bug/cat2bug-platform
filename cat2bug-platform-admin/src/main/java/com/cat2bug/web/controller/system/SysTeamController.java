package com.cat2bug.web.controller.system;

import com.cat2bug.common.annotation.Log;
import com.cat2bug.common.core.controller.BaseController;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.core.page.TableDataInfo;
import com.cat2bug.common.enums.BusinessType;
import com.cat2bug.common.utils.poi.ExcelUtil;
import com.cat2bug.system.domain.SysTeam;
import com.cat2bug.system.service.ISysTeamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 团队Controller
 * 
 * @author yuzhantao
 * @date 2023-11-13
 */
@RestController
@RequestMapping("/system/team")
public class SysTeamController extends BaseController
{
    @Autowired
    private ISysTeamService sysTeamService;

    /**
     * 查询团队列表
     */
    @PreAuthorize("@ss.hasPermi('system:team:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysTeam sysTeam)
    {
        startPage();
        List<SysTeam> list = sysTeamService.selectSysTeamList(sysTeam);
        return getDataTable(list);
    }

    /**
     * 查询团队列表
     */
    @PreAuthorize("@ss.hasPermi('system:team:list')")
    @GetMapping("/my")
    public TableDataInfo my(SysTeam sysTeam)
    {
        startPage();
        List<SysTeam> list = sysTeamService.selectSysTeamListByUserId(getUserId());
        return getDataTable(list);
    }



    /**
     * 导出团队列表
     */
    @PreAuthorize("@ss.hasPermi('system:team:export')")
    @Log(title = "团队", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysTeam sysTeam)
    {
        List<SysTeam> list = sysTeamService.selectSysTeamList(sysTeam);
        ExcelUtil<SysTeam> util = new ExcelUtil<SysTeam>(SysTeam.class);
        util.exportExcel(response, list, "团队数据");
    }

    /**
     * 获取团队详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:team:query')")
    @GetMapping(value = "/{teamId}")
    public AjaxResult getInfo(@PathVariable("teamId") Long teamId)
    {
        return success(sysTeamService.selectSysTeamByTeamId(teamId));
    }

    /**
     * 获取团队成员
     */
    @PreAuthorize("@ss.hasPermi('system:team:query')")
    @GetMapping(value = "/{teamId}/member")
    public AjaxResult getMember(@PathVariable("teamId") Long teamId)
    {
        return success(sysTeamService.selectSysUserListByTeamId(teamId));
    }

    /**
     * 新增团队
     */
    @PreAuthorize("@ss.hasPermi('system:team:add')")
    @Log(title = "团队", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SysTeam sysTeam)
    {
        return toAjax(sysTeamService.insertSysTeam(sysTeam));
    }

    /**
     * 修改团队
     */
    @PreAuthorize("@ss.hasPermi('system:team:edit')")
    @Log(title = "团队", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SysTeam sysTeam)
    {
        return toAjax(sysTeamService.updateSysTeam(sysTeam));
    }

    /**
     * 删除团队
     */
    @PreAuthorize("@ss.hasPermi('system:team:remove')")
    @Log(title = "团队", businessType = BusinessType.DELETE)
	@DeleteMapping("/{teamIds}")
    public AjaxResult remove(@PathVariable Long[] teamIds)
    {
        return toAjax(sysTeamService.deleteSysTeamByTeamIds(teamIds));
    }
}
