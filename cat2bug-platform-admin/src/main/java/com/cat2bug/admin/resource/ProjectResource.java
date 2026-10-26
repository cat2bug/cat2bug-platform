package com.cat2bug.admin.resource;

import com.cat2bug.admin.service.ProjectCloudPushService;
import com.cat2bug.admin.service.SystemExcelExportService;
import com.cat2bug.common.api.AjaxResult;
import com.cat2bug.common.api.TableDataInfo;
import com.cat2bug.common.domain.LoginUser;
import com.cat2bug.common.domain.SysProject;
import com.cat2bug.common.domain.SysRole;
import com.cat2bug.common.domain.SysUserConfig;
import com.cat2bug.common.domain.SysUserProject;
import com.cat2bug.admin.support.InviteLinkUrlSupport;
import com.cat2bug.common.domain.vo.BatchUserRoleVo;
import com.cat2bug.common.domain.vo.CreateMemberInviteVo;
import com.cat2bug.common.domain.vo.PullProject;
import com.cat2bug.system.service.SysMemberInviteService;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.framework.security.AuthContext;
import com.cat2bug.framework.security.RequiresPermissions;
import com.cat2bug.system.service.SysProjectService;
import com.cat2bug.system.service.SysUserConfigService;
import com.cat2bug.system.service.SysRoleService;
import com.cat2bug.system.service.SysUserProjectRoleService;
import com.cat2bug.system.service.SysUserProjectService;
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

import java.util.List;

@Path("/system/project")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProjectResource {

    static final String SPREADSHEET_MEDIA_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Inject
    AuthContext authContext;

    @Inject
    SysProjectService sysProjectService;

    @Inject
    SystemExcelExportService systemExcelExportService;

    @Inject
    SysUserProjectService sysUserProjectService;

    @Inject
    SysRoleService sysRoleService;

    @Inject
    SysUserProjectRoleService sysUserProjectRoleService;

    @Inject
    SysUserConfigService sysUserConfigService;

    @Inject
    ProjectCloudPushService projectCloudPushService;

    @Inject
    SysMemberInviteService sysMemberInviteService;

    @GET
    @Path("/list")
    @RequiresPermissions("system:project:list")
    public TableDataInfo list(@QueryParam("teamId") Long teamId,
                              @QueryParam("projectName") String projectName,
                              @QueryParam("collect") Boolean collect,
                              @QueryParam("memberOnly") Boolean memberOnly,
                              @QueryParam("pageNum") Integer pageNum,
                              @QueryParam("pageSize") Integer pageSize) {
        LoginUser loginUser = requireLoginUser();
        SysProject query = new SysProject();
        query.setTeamId(teamId);
        query.setProjectName(projectName);
        if (collect != null) {
            query.setCollect(collect);
        }
        if (memberOnly != null) {
            query.setMemberOnly(memberOnly);
        }
        return sysProjectService.selectProjectPage(loginUser.getUserId(), query, pageNum, pageSize);
    }

    @POST
    @Path("/export")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(SPREADSHEET_MEDIA_TYPE)
    @RequiresPermissions("system:project:export")
    public Response export(@RestForm("teamId") Long teamId,
                           @RestForm("projectName") String projectName,
                           @RestForm("collect") Boolean collect) {
        LoginUser loginUser = requireLoginUser();
        SysProject query = new SysProject();
        query.setTeamId(teamId);
        query.setProjectName(projectName);
        if (collect != null) {
            query.setCollect(collect);
        }
        byte[] workbook = systemExcelExportService.exportProjects(loginUser.getUserId(), query);
        return Response.ok(workbook).type(SPREADSHEET_MEDIA_TYPE).build();
    }

    @GET
    @Path("/path/{teamKey}/{projectKey}")
    @RequiresPermissions("system:project:query")
    public AjaxResult resolveByPath(@PathParam("teamKey") String teamKey,
                                    @PathParam("projectKey") String projectKey) {
        LoginUser loginUser = requireLoginUser();
        return AjaxResult.success(
                sysProjectService.selectSysProjectByTeamAndProjectKey(teamKey, projectKey, loginUser.getUserId()));
    }

    @GET
    @Path("/{projectId:[0-9]+}")
    @RequiresPermissions("system:project:query")
    public AjaxResult getInfo(@PathParam("projectId") Long projectId) {
        LoginUser loginUser = requireLoginUser();
        return AjaxResult.success(sysProjectService.selectSysProjectByProjectId(projectId, loginUser.getUserId()));
    }

    @GET
    @Path("/{projectId}/role")
    @RequiresPermissions("system:project:list")
    public TableDataInfo projectRoles(@PathParam("projectId") Long projectId,
                                      @QueryParam("pageNum") Integer pageNum,
                                      @QueryParam("pageSize") Integer pageSize) {
        SysRole query = new SysRole();
        query.setIsProjectRole(true);
        // 成员页/创建项目未传分页参数，需一次返回全部项目角色（含 projectCreateBy 等计算字段）
        if (pageNum == null && pageSize == null) {
            var rows = sysRoleService.selectRoleList(query);
            return new TableDataInfo(rows, rows.size());
        }
        return sysRoleService.selectRolePage(query, pageNum, pageSize);
    }

    @GET
    @Path("/{projectId}/member")
    @RequiresPermissions(value = {"system:project:list", "system:project:member"}, logical = RequiresPermissions.Logical.OR)
    public TableDataInfo memberList(@PathParam("projectId") Long projectId,
                                      @QueryParam("params[search]") String search,
                                      @QueryParam("pageNum") Integer pageNum,
                                      @QueryParam("pageSize") Integer pageSize) {
        return sysUserProjectService.selectMemberPage(projectId, search, pageNum, pageSize);
    }

    @GET
    @Path("/{projectId}/not-member")
    @RequiresPermissions("system:project:member:add")
    public TableDataInfo notMemberList(@PathParam("projectId") Long projectId,
                                       @QueryParam("params[search]") String search,
                                       @QueryParam("pageNum") Integer pageNum,
                                       @QueryParam("pageSize") Integer pageSize) {
        return sysUserProjectService.selectNotMemberPage(projectId, search, pageNum, pageSize);
    }

    @POST
    @RequiresPermissions("system:project:add")
    public AjaxResult add(SysProject sysProject) {
        LoginUser loginUser = requireLoginUser();
        return toAjax(sysProjectService.insertSysProject(sysProject, loginUser.getUserId()));
    }

    @PUT
    @RequiresPermissions("system:project:edit")
    public AjaxResult edit(SysProject sysProject) {
        LoginUser loginUser = requireLoginUser();
        return toAjax(sysProjectService.updateSysProject(sysProject, loginUser.getUserId()));
    }

    @DELETE
    @Path("/{projectId}")
    @RequiresPermissions("system:project:remove")
    public AjaxResult remove(@PathParam("projectId") Long projectId) {
        return toAjax(sysProjectService.deleteSysProjectByProjectId(projectId));
    }

    @POST
    @Path("/{projectId:[0-9]+}/invite-link")
    @RequiresPermissions("system:project:member:add")
    public AjaxResult createInviteLink(@PathParam("projectId") Long projectId,
                                       CreateMemberInviteVo request,
                                       @Context UriInfo uriInfo) {
        LoginUser loginUser = requireLoginUser();
        return AjaxResult.success(sysMemberInviteService.createProjectInviteLink(
                projectId, request, loginUser.getUserId(), InviteLinkUrlSupport.resolveInviteBaseUrl(uriInfo)));
    }

    @GET
    @Path("/{projectId:[0-9]+}/invite-links")
    @RequiresPermissions("system:project:member:add")
    public AjaxResult listInviteLinks(@PathParam("projectId") Long projectId, @Context UriInfo uriInfo) {
        return AjaxResult.success(sysMemberInviteService.listActiveProjectInviteLinks(
                projectId, InviteLinkUrlSupport.resolveInviteBaseUrl(uriInfo)));
    }

    @DELETE
    @Path("/{projectId:[0-9]+}/invite-link/{inviteId}")
    @RequiresPermissions("system:project:member:add")
    public AjaxResult revokeInviteLink(@PathParam("projectId") Long projectId,
                                       @PathParam("inviteId") Long inviteId) {
        return toAjax(sysMemberInviteService.revokeProjectInviteLink(projectId, inviteId));
    }

    @POST
    @Path("/{projectId}/member")
    @RequiresPermissions("system:project:member:add")
    public AjaxResult addMember(@PathParam("projectId") Long projectId, BatchUserRoleVo body) {
        if (body == null) {
            throw new ApiException("成员不能为空");
        }
        body.setProjectId(projectId);
        return toAjax(sysUserProjectService.batchInsertSysUserProject(body));
    }

    @POST
    @Path("/{projectId}/push")
    @RequiresPermissions("system:project:push")
    public AjaxResult push(@PathParam("projectId") Long projectId, PullProject pullProject) throws java.io.IOException {
        LoginUser loginUser = requireLoginUser();
        SysUserConfig userConfig = sysUserConfigService.selectSysUserConfigByUserId(loginUser.getUserId());
        Long currentProjectId = userConfig != null ? userConfig.getCurrentProjectId() : projectId;
        boolean ok = projectCloudPushService.pushToCloud(currentProjectId, pullProject.getPullKey());
        return toAjax(ok ? 1 : 0);
    }

    @POST
    @Path("/{projectId}/collect")
    @RequiresPermissions("system:project:list")
    public AjaxResult collect(@PathParam("projectId") Long projectId, SysProject sysProject) {
        LoginUser loginUser = requireLoginUser();
        SysUserProject up = new SysUserProject();
        up.setUserId(loginUser.getUserId());
        up.setProjectId(projectId);
        up.setCollect(sysProject.isCollect());
        return toAjax(sysUserProjectService.updateCollect(up));
    }

    @PUT
    @Path("/{projectId}/member/{memberId}/role")
    @RequiresPermissions("system:project:member:update")
    public AjaxResult updateMemberRole(@PathParam("projectId") Long projectId,
                                       @PathParam("memberId") Long memberId,
                                       List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            throw new ApiException("成员角色不能为空!");
        }
        return toAjax(sysUserProjectRoleService.updateSysUserProjectRole(projectId, memberId, roleIds));
    }

    @DELETE
    @Path("/{projectId}/member/{memberId}")
    @RequiresPermissions("system:project:member:remove")
    public AjaxResult removeMember(@PathParam("projectId") Long projectId,
                                   @PathParam("memberId") Long memberId) {
        LoginUser loginUser = requireLoginUser();
        return toAjax(sysUserProjectService.deleteMember(projectId, memberId, loginUser.getUserId()));
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
