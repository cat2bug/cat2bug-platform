package com.cat2bug.web.controller.system;

import com.cat2bug.common.annotation.Log;
import com.cat2bug.common.core.controller.BaseController;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.core.domain.entity.SysRole;
import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.common.core.page.TableDataInfo;
import com.cat2bug.common.enums.BusinessType;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.system.domain.SysTeam;
import com.cat2bug.system.domain.SysUserTeam;
import com.cat2bug.system.domain.vo.BatchUserRoleVo;
import com.cat2bug.system.domain.vo.CreateMemberInviteVo;
import com.cat2bug.system.service.ISysMemberInviteService;
import com.cat2bug.system.service.ISysRoleService;
import com.cat2bug.system.service.ISysTeamService;
import com.cat2bug.system.service.ISysUserTeamRoleService;
import com.cat2bug.system.service.ISysUserTeamService;
import com.cat2bug.system.support.TeamMembershipSupport;
import com.cat2bug.web.util.InviteLinkUrlSupport;
import com.cat2bug.web.excel.ExcelHttpSupport;
import com.cat2bug.web.service.excel.SystemExcelExportService;
import org.springframework.beans.factory.annotation.Autowired;
import java.io.IOException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
    private SystemExcelExportService systemExcelExportService;

    @Autowired
    private ISysTeamService sysTeamService;

    @Autowired
    private ISysUserTeamService sysUserTeamService;

    @Autowired
    private ISysUserTeamRoleService sysUserTeamRoleService;

    @Autowired
    private ISysMemberInviteService sysMemberInviteService;

    @Autowired
    private TeamMembershipSupport teamMembershipSupport;

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
        teamMembershipSupport.requireTeamMember(getUserId(), teamId);
        startPage();
        List<SysUser> list = sysTeamService.selectSysUserListByTeamIdAndSysUser(teamId, sysUser);
        TableDataInfo ret = getDataTable(list);
        ret.setRows(list.stream().map(u->{
            if(u.getRoles()!=null){
                u.setRoleIds(u.getRoles().stream().map(r->r.getRoleId()).collect(Collectors.toList()).toArray(new Long[]{}));
            }
            return u;
        }).collect(Collectors.toList()));

        return ret;
    }

    /**
     * 获取团队成员列表
     */
    @GetMapping("/{teamId}/not-member")
    public TableDataInfo notTeamListMember(@PathVariable("teamId") Long teamId, SysUser sysUser)
    {
        teamMembershipSupport.requireTeamMember(getUserId(), teamId);
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
    public void export(HttpServletResponse response, SysTeam sysTeam) throws IOException
    {
        ExcelHttpSupport.write(response, systemExcelExportService.exportTeams(sysTeam), "团队数据.xlsx");
    }

    /**
     * 获取团队详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:team:query')")
    @GetMapping(value = "/{teamId}")
    public AjaxResult getInfo(@PathVariable("teamId") Long teamId)
    {
        teamMembershipSupport.requireTeamMember(getUserId(), teamId);
        return success(sysTeamService.selectSysTeamByTeamId(teamId));
    }

    @PreAuthorize("@ss.hasPermi('system:team:query')")
    @GetMapping(value = "/{teamId}/role")
    public AjaxResult getRoleList(@PathVariable("teamId") Long teamId)
    {
        teamMembershipSupport.requireTeamMember(getUserId(), teamId);
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
        teamMembershipSupport.requireTeamMember(getUserId(), teamId);
        batchUserRoleVo.setTeamId(teamId);
        return toAjax(sysTeamService.inviteMember(batchUserRoleVo));
    }

    @PreAuthorize("@ss.hasPermi('system:team:member:invite')")
    @Log(title = "团队邀请链接", businessType = BusinessType.INSERT)
    @PostMapping("/{teamId}/invite-link")
    public AjaxResult createInviteLink(@PathVariable("teamId") Long teamId,
                                       @RequestBody CreateMemberInviteVo request,
                                       HttpServletRequest httpRequest)
    {
        teamMembershipSupport.requireTeamMember(getUserId(), teamId);
        return success(sysMemberInviteService.createTeamInviteLink(
                teamId, request, getUserId(), InviteLinkUrlSupport.resolveInviteBaseUrl(httpRequest)));
    }

    @PreAuthorize("@ss.hasPermi('system:team:member:invite')")
    @GetMapping("/{teamId}/invite-links")
    public AjaxResult listInviteLinks(@PathVariable("teamId") Long teamId, HttpServletRequest httpRequest)
    {
        teamMembershipSupport.requireTeamMember(getUserId(), teamId);
        return success(sysMemberInviteService.listActiveTeamInviteLinks(
                teamId, InviteLinkUrlSupport.resolveInviteBaseUrl(httpRequest)));
    }

    @PreAuthorize("@ss.hasPermi('system:team:member:invite')")
    @Log(title = "团队邀请链接", businessType = BusinessType.DELETE)
    @DeleteMapping("/{teamId}/invite-link/{inviteId}")
    public AjaxResult revokeInviteLink(@PathVariable("teamId") Long teamId,
                                       @PathVariable("inviteId") Long inviteId)
    {
        teamMembershipSupport.requireTeamMember(getUserId(), teamId);
        return toAjax(sysMemberInviteService.revokeTeamInviteLink(teamId, inviteId));
    }

    @PreAuthorize("@ss.hasPermi('system:team:lock')")
    @Log(title = "团队", businessType = BusinessType.UPDATE)
    @PutMapping("/{teamId}/lock")
    public AjaxResult lock(@PathVariable("teamId") Long teamId, @RequestBody SysTeam team)
    {
        teamMembershipSupport.requireTeamMember(getUserId(), teamId);
        SysTeam updateTeam = new SysTeam();
        updateTeam.setTeamId(teamId);
        updateTeam.setLock(team.getLock());
        updateTeam.setLockRemark(team.getLockRemark());
        return toAjax(sysTeamService.updateSysTeam(updateTeam));
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
        teamMembershipSupport.requireTeamMember(getUserId(), teamId);
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
        teamMembershipSupport.requireTeamMember(getUserId(), sysTeam.getTeamId());
        sysTeam.setLock(null);
        sysTeam.setLockRemark(null);
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
        teamMembershipSupport.requireTeamMember(getUserId(), teamId);
        return toAjax(sysUserTeamService.updateSysUserTeamByTeamIdAndMemberId(teamId, memberId, sysUserTeam));
    }

    @PreAuthorize("@ss.hasPermi('system:team:edit')")
    @Log(title = "团队", businessType = BusinessType.UPDATE)
    @PutMapping("/{teamId}/member/{memberId}/roles")
    public AjaxResult editTeamMemberRoleIds(@PathVariable Long teamId, @PathVariable Long memberId,  @RequestBody Long[] roleIds)
    {
        teamMembershipSupport.requireTeamMember(getUserId(), teamId);
        return toAjax(sysUserTeamRoleService.updateSysUserTeamRoleByTeamIdAndMemberIdAndRoleIds(teamId, memberId, roleIds));
    }

    /**
     * 删除团队
     */
    @PreAuthorize("@ss.hasPermi('system:team:remove')")
    @Log(title = "团队", businessType = BusinessType.DELETE)
	@DeleteMapping("/{teamIds}")
    public AjaxResult remove(@PathVariable Long[] teamIds)
    {
        teamMembershipSupport.requireTeamMember(getUserId(), teamIds);
        return toAjax(sysTeamService.deleteSysTeamByTeamIds(teamIds));
    }
}
