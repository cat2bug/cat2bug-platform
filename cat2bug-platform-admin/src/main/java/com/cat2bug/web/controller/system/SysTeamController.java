package com.cat2bug.web.controller.system;

import com.cat2bug.common.annotation.Log;
import com.cat2bug.common.core.controller.BaseController;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.core.domain.entity.SysRole;
import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.common.core.page.TableDataInfo;
import com.cat2bug.common.enums.BusinessType;
import com.cat2bug.common.utils.StringUtils;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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
    private ISysRoleService roleService;

    @Autowired
    private ISysTeamService sysTeamService;

    @Autowired
    private ISysUserTeamService sysUserTeamService;

    @Autowired
    private ISysUserTeamRoleService sysUserTeamRoleService;

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
     * 获取团队成员列表
     */
    @GetMapping("/{teamId}/member")
    public TableDataInfo listMember(@PathVariable("teamId") Long teamId, SysUser sysUser)
    {
        startPage();
        List<SysUser> list = sysTeamService.selectSysUserListByTeamIdAndSysUser(teamId, sysUser);
        list = list.stream().map(u->{
            if(u.getRoles()!=null){
                u.setRoleIds(u.getRoles().stream().map(r->r.getRoleId()).collect(Collectors.toList()).toArray(new Long[]{}));
            }
            return u;
        }).collect(Collectors.toList());
        return getDataTable(list);
    }

    /**
     * 获取团队成员列表
     */
    @GetMapping("/{teamId}/not-member")
    public TableDataInfo notTeamListMember(@PathVariable("teamId") Long teamId, SysUser sysUser)
    {
        startPage();
        List<SysUser> list = sysTeamService.selectSysUserListByTeamIdAndNotSysUser(teamId, sysUser);
        list = list.stream().map(u->{
            if(u.getRoles()!=null){
                u.setRoleIds(u.getRoles().stream().map(r->r.getRoleId()).collect(Collectors.toList()).toArray(new Long[]{}));
            }
            return u;
        }).collect(Collectors.toList());
        return getDataTable(list);
    }

    /**
     * 查询我所拥有的团队
     */
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

    @PreAuthorize("@ss.hasPermi('system:team:query')")
    @GetMapping(value = "/{teamId}/role")
    public AjaxResult getRoleList(@PathVariable("teamId") Long teamId)
    {
        SysRole role = new SysRole();
        role.setIsTeamRole(true);
        return success(roleService.selectRoleList(role));
    }

    /**
     * 新增团队
     */
    @PreAuthorize("@ss.hasPermi('system:team:add')")
    @Log(title = "团队", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SysTeam sysTeam)
    {
        return success(sysTeamService.insertSysTeam(sysTeam));
    }

    /**
     * 邀请团队成员
     */
    @PreAuthorize("@ss.hasPermi('system:team:member:invite')")
    @Log(title = "团队", businessType = BusinessType.INSERT)
    @PostMapping("/{teamId}/invite")
    public AjaxResult inviteMember(@PathVariable("teamId") Long teamId, @RequestBody BatchUserRoleVo batchUserRoleVo)
    {
        batchUserRoleVo.setTeamId(teamId);
        return toAjax(sysTeamService.inviteMember(batchUserRoleVo));
    }

    /**
     * 新建团队成员
     * @param teamId
     * @param sysUser
     * @return
     */
    @PreAuthorize("@ss.hasPermi('system:team:member:create')")
    @Log(title = "团队成员", businessType = BusinessType.INSERT)
    @PostMapping("/{teamId}/member")
    public AjaxResult createMember(@PathVariable Long teamId,@RequestBody SysUser sysUser)
    {
        return toAjax(sysTeamService.insertSysUser(teamId, sysUser));
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
     * 修改团队成员权限
     */
    @PreAuthorize("@ss.hasPermi('system:team:edit')")
    @Log(title = "团队", businessType = BusinessType.UPDATE)
    @PutMapping("/{teamId}/member/{memberId}/role")
    public AjaxResult editTeamMemberRole(@PathVariable Long teamId, @PathVariable Long memberId, @RequestBody SysUserTeam sysUserTeam)
    {
        return toAjax(sysUserTeamService.updateSysUserTeamByTeamIdAndMemberId(teamId, memberId, sysUserTeam));
    }

    @PreAuthorize("@ss.hasPermi('system:team:edit')")
    @Log(title = "团队", businessType = BusinessType.UPDATE)
    @PutMapping("/{teamId}/member/{memberId}/roles")
    public AjaxResult editTeamMemberRoleIds(@PathVariable Long teamId, @PathVariable Long memberId,  @RequestBody Long[] roleIds)
    {
        return toAjax(sysUserTeamRoleService.updateSysUserTeamRoleByTeamIdAndMemberIdAndRoleIds(teamId, memberId, roleIds));
    }

    /**
     * 删除团队
     */
    @PreAuthorize("@ss.hasPermi('system:team:delete')")
    @Log(title = "团队", businessType = BusinessType.DELETE)
	@DeleteMapping("/{teamIds}")
    public AjaxResult remove(@PathVariable Long[] teamIds)
    {
        return toAjax(sysTeamService.deleteSysTeamByTeamIds(teamIds));
    }
}
