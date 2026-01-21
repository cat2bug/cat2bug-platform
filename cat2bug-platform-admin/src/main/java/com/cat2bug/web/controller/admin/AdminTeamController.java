package com.cat2bug.web.controller.admin;

import com.cat2bug.common.annotation.Log;
import com.cat2bug.common.core.controller.BaseController;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.core.domain.entity.SysRole;
import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.common.core.page.TableDataInfo;
import com.cat2bug.common.enums.BusinessType;
import com.cat2bug.common.utils.poi.ExcelUtil;
import com.cat2bug.system.domain.SysTeam;
import com.cat2bug.system.domain.SysUserTeam;
import com.cat2bug.system.domain.vo.BatchUserRoleVo;
import com.cat2bug.system.service.ISysRoleService;
import com.cat2bug.system.service.ISysTeamService;
import com.cat2bug.system.service.ISysUserTeamRoleService;
import com.cat2bug.system.service.ISysUserTeamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 团队Controller
 * 
 * @author yuzhantao
 * @date 2023-11-13
 */
@RestController
@RequestMapping("/admin/team")
public class AdminTeamController extends BaseController
{
    @Autowired
    private ISysTeamService sysTeamService;

    /**
     * 查询团队列表
     */
    @PreAuthorize("@ss.hasPermi('admin:team:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysTeam sysTeam)
    {
        startPage();
        List<SysTeam> list = sysTeamService.selectSysTeamList(sysTeam);
        return getDataTable(list);
    }

    /**
     * 邀请团队成员
     */
    @PreAuthorize("@ss.hasPermi('admin:team:lock')")
    @Log(title = "团队", businessType = BusinessType.UPDATE)
    @PutMapping("/{teamId}/lock")
    public AjaxResult lock(@PathVariable("teamId") Long teamId, @RequestBody SysTeam team)
    {
        SysTeam updateTeam = new SysTeam();
        updateTeam.setTeamId(teamId);
        updateTeam.setLock(team.getLock());
        updateTeam.setLockRemark(team.getLockRemark());
        return toAjax(sysTeamService.updateSysTeam(updateTeam));
    }
}
