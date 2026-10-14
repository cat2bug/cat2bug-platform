package com.cat2bug.quarkus.admin.resource;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.cat2bug.quarkus.admin.service.SystemExcelExportService;
import com.cat2bug.quarkus.common.api.AjaxResult;
import com.cat2bug.quarkus.common.api.TableDataInfo;
import com.cat2bug.quarkus.common.domain.LoginUser;
import com.cat2bug.quarkus.common.domain.SysRole;
import com.cat2bug.quarkus.common.domain.SysTeam;
import com.cat2bug.quarkus.common.domain.SysUser;
import com.cat2bug.quarkus.common.domain.SysUserTeam;
import com.cat2bug.quarkus.admin.support.InviteLinkUrlSupport;
import com.cat2bug.quarkus.common.domain.vo.BatchUserRoleVo;
import com.cat2bug.quarkus.common.domain.vo.CreateMemberInviteVo;
import com.cat2bug.quarkus.system.service.SysMemberInviteService;
import com.cat2bug.quarkus.common.exception.ApiException;
import com.cat2bug.quarkus.common.util.StringUtils;
import com.cat2bug.quarkus.framework.security.AuthContext;
import com.cat2bug.quarkus.framework.security.RequiresPermissions;
import com.cat2bug.quarkus.system.service.SysRoleService;
import com.cat2bug.quarkus.system.service.SysTeamService;
import com.cat2bug.quarkus.system.service.SysUserTeamRoleService;
import com.cat2bug.quarkus.system.service.SysUserTeamService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.jboss.resteasy.reactive.RestForm;

import java.util.Arrays;

@Path("/system/team")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TeamResource {

    static final String SPREADSHEET_MEDIA_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Inject
    AuthContext authContext;

    @Inject
    SysTeamService sysTeamService;

    @Inject
    SystemExcelExportService systemExcelExportService;

    @Inject
    SysRoleService sysRoleService;

    @Inject
    SysUserTeamService sysUserTeamService;

    @Inject
    SysUserTeamRoleService sysUserTeamRoleService;

    @Inject
    SysMemberInviteService sysMemberInviteService;

    @GET
    @Path("/list")
    @RequiresPermissions("system:team:list")
    public TableDataInfo list(@QueryParam("teamName") String teamName,
                              @QueryParam("pageNum") Integer pageNum,
                              @QueryParam("pageSize") Integer pageSize) {
        return sysTeamService.selectTeamPage(teamName, pageNum, pageSize);
    }

    @GET
    @Path("/my")
    public TableDataInfo my(@QueryParam("pageNum") Integer pageNum,
                            @QueryParam("pageSize") Integer pageSize) {
        LoginUser loginUser = requireLoginUser();
        return sysTeamService.selectMyTeams(loginUser.getUserId(), pageNum, pageSize);
    }

    @POST
    @Path("/export")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(SPREADSHEET_MEDIA_TYPE)
    @RequiresPermissions("system:team:export")
    public Response export(@RestForm("teamName") String teamName) {
        byte[] workbook = systemExcelExportService.exportTeams(teamName);
        return Response.ok(workbook).type(SPREADSHEET_MEDIA_TYPE).build();
    }

    @GET
    @Path("/{teamId}")
    @RequiresPermissions("system:team:query")
    public AjaxResult getInfo(@PathParam("teamId") Long teamId) {
        return AjaxResult.success(sysTeamService.selectSysTeamByTeamId(teamId));
    }

    @GET
    @Path("/{teamId}/member")
    public TableDataInfo listMember(@PathParam("teamId") Long teamId,
                                    @QueryParam("params[search]") String search,
                                    @QueryParam("pageNum") Integer pageNum,
                                    @QueryParam("pageSize") Integer pageSize) {
        return sysTeamService.selectTeamMemberPage(teamId, search, pageNum, pageSize);
    }

    @GET
    @Path("/{teamId}/role")
    @RequiresPermissions("system:team:query")
    public AjaxResult getRoleList(@PathParam("teamId") Long teamId) {
        SysRole role = new SysRole();
        role.setIsTeamRole(true);
        return AjaxResult.success(sysRoleService.selectRoleList(role));
    }

    @GET
    @Path("/{teamId}/not-member")
    public TableDataInfo notMember(@PathParam("teamId") Long teamId,
                                   @QueryParam("params[search]") String search,
                                   @QueryParam("pageNum") Integer pageNum,
                                   @QueryParam("pageSize") Integer pageSize) {
        return sysTeamService.selectTeamNotMemberPage(teamId, search, pageNum, pageSize);
    }

    @POST
    @RequiresPermissions("system:team:add")
    public AjaxResult add(SysTeam sysTeam) {
        LoginUser loginUser = requireLoginUser();
        SysTeam created = sysTeamService.insertSysTeam(
                sysTeam, loginUser.getUserId(), loginUser.getUsername());
        return AjaxResult.success(created);
    }

    @POST
    @Path("/{teamId}/member")
    @RequiresPermissions("system:team:member:create")
    public AjaxResult createMember(@PathParam("teamId") Long teamId, SysUser sysUser) {
        sysUser.setPhoneNumber(StringUtils.trimToNull(sysUser.getPhoneNumber()));
        sysUser.setEmail(StringUtils.trimToNull(sysUser.getEmail()));
        LoginUser loginUser = requireLoginUser();
        sysUser.setCreateBy(loginUser.getUsername());
        if (StringUtils.isNotEmpty(sysUser.getPassword())) {
            sysUser.setPassword(BCrypt.withDefaults().hashToString(12, sysUser.getPassword().toCharArray()));
        }
        return toAjax(sysTeamService.insertSysUser(teamId, sysUser));
    }

    @POST
    @Path("/{teamId}/invite")
    @RequiresPermissions("system:team:member:invite")
    public AjaxResult inviteMember(@PathParam("teamId") Long teamId, BatchUserRoleVo batchUserRoleVo) {
        batchUserRoleVo.setTeamId(teamId);
        LoginUser loginUser = requireLoginUser();
        return toAjax(sysTeamService.inviteMember(batchUserRoleVo, String.valueOf(loginUser.getUserId())));
    }

    @POST
    @Path("/{teamId}/invite-link")
    @RequiresPermissions("system:team:member:invite")
    public AjaxResult createInviteLink(@PathParam("teamId") Long teamId,
                                       CreateMemberInviteVo request,
                                       @Context UriInfo uriInfo) {
        LoginUser loginUser = requireLoginUser();
        return AjaxResult.success(sysMemberInviteService.createTeamInviteLink(
                teamId, request, loginUser.getUserId(), InviteLinkUrlSupport.resolveInviteBaseUrl(uriInfo)));
    }

    @GET
    @Path("/{teamId}/invite-links")
    @RequiresPermissions("system:team:member:invite")
    public AjaxResult listInviteLinks(@PathParam("teamId") Long teamId, @Context UriInfo uriInfo) {
        return AjaxResult.success(sysMemberInviteService.listActiveTeamInviteLinks(
                teamId, InviteLinkUrlSupport.resolveInviteBaseUrl(uriInfo)));
    }

    @DELETE
    @Path("/{teamId}/invite-link/{inviteId}")
    @RequiresPermissions("system:team:member:invite")
    public AjaxResult revokeInviteLink(@PathParam("teamId") Long teamId,
                                       @PathParam("inviteId") Long inviteId) {
        return toAjax(sysMemberInviteService.revokeTeamInviteLink(teamId, inviteId));
    }

    @PUT
    @Path("/{teamId}/lock")
    @RequiresPermissions("system:team:lock")
    public AjaxResult lock(@PathParam("teamId") Long teamId, SysTeam team) {
        SysTeam updateTeam = new SysTeam();
        updateTeam.setTeamId(teamId);
        updateTeam.setLock(team.getLock());
        updateTeam.setLockRemark(team.getLockRemark());
        return toAjax(sysTeamService.updateSysTeam(updateTeam));
    }

    @PUT
    @Path("/{teamId}/member/{memberId}/role")
    @RequiresPermissions("system:team:edit")
    public AjaxResult editTeamMemberRole(@PathParam("teamId") Long teamId,
                                         @PathParam("memberId") Long memberId,
                                         SysUserTeam sysUserTeam) {
        return toAjax(sysUserTeamService.updateSysUserTeamByTeamIdAndMemberId(teamId, memberId, sysUserTeam));
    }

    @PUT
    @Path("/{teamId}/member/{memberId}/roles")
    @RequiresPermissions("system:team:edit")
    public AjaxResult editTeamMemberRoleIds(@PathParam("teamId") Long teamId,
                                            @PathParam("memberId") Long memberId,
                                            Long[] roleIds) {
        return toAjax(sysUserTeamRoleService.updateSysUserTeamRoleByTeamIdAndMemberIdAndRoleIds(
                teamId, memberId, roleIds));
    }

    @PUT
    @RequiresPermissions("system:team:edit")
    public AjaxResult edit(SysTeam sysTeam) {
        sysTeam.setLock(null);
        sysTeam.setLockRemark(null);
        return toAjax(sysTeamService.updateSysTeam(sysTeam));
    }

    @DELETE
    @Path("/{teamIds}")
    @RequiresPermissions("system:team:remove")
    public AjaxResult remove(@PathParam("teamIds") String teamIds) {
        Long[] ids = Arrays.stream(teamIds.split(","))
                .map(String::trim)
                .map(Long::valueOf)
                .toArray(Long[]::new);
        return toAjax(sysTeamService.deleteSysTeamByTeamIds(ids));
    }

    private LoginUser requireLoginUser() {
        LoginUser loginUser = authContext.getLoginUser();
        if (loginUser == null) {
            throw new ApiException(401, "未登录或会话已失效");
        }
        return loginUser;
    }

    private AjaxResult toAjax(int rows) {
        return rows > 0 ? AjaxResult.success() : AjaxResult.error("操作失败");
    }
}
