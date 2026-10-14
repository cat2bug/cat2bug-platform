package com.cat2bug.quarkus.admin.resource;

import com.cat2bug.quarkus.admin.support.InviteLinkUrlSupport;
import com.cat2bug.quarkus.common.api.AjaxResult;
import com.cat2bug.quarkus.common.domain.SysGitRepository;
import com.cat2bug.quarkus.common.domain.SysGitRepositoryMember;
import com.cat2bug.quarkus.common.domain.git.GitRepoMembersFromRepoResult;
import com.cat2bug.quarkus.common.domain.git.GitRepoMyAccess;
import com.cat2bug.quarkus.common.domain.git.SysGitRemoteListRequest;
import com.cat2bug.quarkus.common.domain.git.SysGitRemoteRepository;
import com.cat2bug.quarkus.common.domain.git.SysGitRepoInviteCreateRequest;
import com.cat2bug.quarkus.common.domain.git.SysGitRepoMembersFromRepoRequest;
import com.cat2bug.quarkus.common.domain.git.SysGitRepositoryImportRequest;
import com.cat2bug.quarkus.common.domain.git.SysGitRepositoryMemberRequest;
import com.cat2bug.quarkus.common.util.MessageUtils;
import com.cat2bug.quarkus.framework.security.RequiresPermissions;
import com.cat2bug.quarkus.framework.security.SecurityUtils;
import com.cat2bug.quarkus.system.service.ISysGitRepositoryMemberService;
import com.cat2bug.quarkus.system.service.ISysGitRepositoryService;
import com.cat2bug.quarkus.system.service.SysMemberInviteService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;

import java.util.List;

@Path("/system/git/repo")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GitRepositoryResource {

    @Inject
    ISysGitRepositoryService sysGitRepositoryService;

    @Inject
    ISysGitRepositoryMemberService sysGitRepositoryMemberService;

    @Inject
    SysMemberInviteService sysMemberInviteService;

    @GET
    @Path("/list")
    @RequiresPermissions(value = {"system:git:repo:list", "system:git:repo:pull"},
            logical = RequiresPermissions.Logical.OR)
    public AjaxResult list(@QueryParam("projectId") Long projectId,
                           @QueryParam("name") String name,
                           @QueryParam("slug") String slug,
                           @QueryParam("visibility") String visibility) {
        SysGitRepository query = new SysGitRepository();
        query.setProjectId(projectId);
        query.setName(name);
        query.setSlug(slug);
        query.setVisibility(visibility);
        return AjaxResult.success(sysGitRepositoryService.selectSysGitRepositoryList(query));
    }

    @GET
    @Path("/{repositoryId:[0-9]+}")
    @RequiresPermissions(value = {"system:git:repo:list", "system:git:repo:pull"},
            logical = RequiresPermissions.Logical.OR)
    public AjaxResult getInfo(@PathParam("repositoryId") Long repositoryId) {
        return AjaxResult.success(sysGitRepositoryService.selectSysGitRepositoryByRepositoryId(repositoryId));
    }

    @POST
    @RequiresPermissions("system:git:repo:manage")
    public AjaxResult add(SysGitRepository body) {
        return AjaxResult.success(sysGitRepositoryService.insertSysGitRepository(body));
    }

    @POST
    @Path("/import")
    @RequiresPermissions("system:git:repo:manage")
    public AjaxResult importRepository(SysGitRepositoryImportRequest request) {
        return AjaxResult.success(sysGitRepositoryService.importSysGitRepository(request));
    }

    @POST
    @Path("/remote/list")
    @RequiresPermissions("system:git:repo:manage")
    public AjaxResult listRemote(SysGitRemoteListRequest request) {
        List<SysGitRemoteRepository> list = sysGitRepositoryService.listRemoteRepositories(request);
        return AjaxResult.success(list);
    }

    @PUT
    @RequiresPermissions("system:git:repo:manage")
    public AjaxResult edit(SysGitRepository body) {
        SysGitRepository updated = sysGitRepositoryService.updateSysGitRepository(body);
        AjaxResult result = AjaxResult.success(updated);
        result.put("cloneUrlChanged", updated.isSlugChanged());
        if (updated.isSlugChanged()) {
            result.put("message", MessageUtils.message("git.repo.slug-changed-hint"));
        }
        return result;
    }

    @DELETE
    @Path("/{repositoryIds}")
    @RequiresPermissions("system:git:repo:manage")
    public AjaxResult remove(@PathParam("repositoryIds") String repositoryIds,
                             @QueryParam("wipeDisk") @DefaultValue("true") boolean wipeDisk) {
        Long[] ids = parseIds(repositoryIds);
        return toAjax(sysGitRepositoryService.deleteSysGitRepositoryByRepositoryIds(ids, wipeDisk));
    }

    @GET
    @Path("/project/{projectId}/overview")
    @RequiresPermissions(value = {"system:git:repo:list", "system:git:repo:pull"},
            logical = RequiresPermissions.Logical.OR)
    public AjaxResult projectOverview(@PathParam("projectId") Long projectId) {
        return AjaxResult.success(sysGitRepositoryService.selectProjectOverview(projectId));
    }

    @GET
    @Path("/project/{projectId}/members")
    @RequiresPermissions(value = {"system:git:repo:list", "system:git:repo:pull"},
            logical = RequiresPermissions.Logical.OR)
    public AjaxResult projectMembers(@PathParam("projectId") Long projectId) {
        return AjaxResult.success(sysGitRepositoryService.selectProjectGitMembers(projectId));
    }

    @POST
    @Path("/{repositoryId:[0-9]+}/collect")
    @RequiresPermissions(value = {"system:git:repo:list", "system:git:repo:pull"},
            logical = RequiresPermissions.Logical.OR)
    public AjaxResult collect(@PathParam("repositoryId") Long repositoryId, SysGitRepository body) {
        boolean collect = body != null && Boolean.TRUE.equals(body.getCollect());
        return toAjax(sysGitRepositoryService.collectSysGitRepository(repositoryId, collect));
    }

    @GET
    @Path("/{repositoryId:[0-9]+}/my-access")
    @RequiresPermissions(value = {"system:git:repo:list", "system:git:repo:pull", "system:git:repo:manage"},
            logical = RequiresPermissions.Logical.OR)
    public AjaxResult myAccess(@PathParam("repositoryId") Long repositoryId) {
        GitRepoMyAccess access = sysGitRepositoryMemberService.getMyAccess(repositoryId);
        return AjaxResult.success(access);
    }

    @GET
    @Path("/{repositoryId:[0-9]+}/members")
    @RequiresPermissions(value = {"system:git:repo:manage", "system:git:repo:list", "system:git:repo:pull"},
            logical = RequiresPermissions.Logical.OR)
    public AjaxResult listMembers(@PathParam("repositoryId") Long repositoryId) {
        List<SysGitRepositoryMember> list = sysGitRepositoryMemberService.selectMembersByRepositoryId(repositoryId);
        return AjaxResult.success(list);
    }

    @POST
    @Path("/{repositoryId:[0-9]+}/members")
    @RequiresPermissions(value = {"system:git:repo:manage", "system:git:repo:list", "system:git:repo:pull"},
            logical = RequiresPermissions.Logical.OR)
    public AjaxResult addMember(@PathParam("repositoryId") Long repositoryId, SysGitRepositoryMemberRequest body) {
        Long userId = body != null ? body.getUserId() : null;
        String role = body != null ? body.getRole() : null;
        return toAjax(sysGitRepositoryMemberService.insertMember(repositoryId, userId, role));
    }

    @PUT
    @Path("/{repositoryId:[0-9]+}/members/{userId}")
    @RequiresPermissions(value = {"system:git:repo:manage", "system:git:repo:list", "system:git:repo:pull"},
            logical = RequiresPermissions.Logical.OR)
    public AjaxResult updateMember(@PathParam("repositoryId") Long repositoryId,
                                   @PathParam("userId") Long userId,
                                   SysGitRepositoryMemberRequest body) {
        String role = body != null ? body.getRole() : null;
        return toAjax(sysGitRepositoryMemberService.updateMemberRole(repositoryId, userId, role));
    }

    @DELETE
    @Path("/{repositoryId:[0-9]+}/members/{userId}")
    @RequiresPermissions(value = {"system:git:repo:manage", "system:git:repo:list", "system:git:repo:pull"},
            logical = RequiresPermissions.Logical.OR)
    public AjaxResult removeMember(@PathParam("repositoryId") Long repositoryId,
                                   @PathParam("userId") Long userId) {
        return toAjax(sysGitRepositoryMemberService.deleteMember(repositoryId, userId));
    }

    @GET
    @Path("/{repositoryId:[0-9]+}/invites")
    @RequiresPermissions(value = {"system:git:repo:manage", "system:git:repo:list", "system:git:repo:pull"},
            logical = RequiresPermissions.Logical.OR)
    public AjaxResult listInvites(@PathParam("repositoryId") Long repositoryId, @Context UriInfo uriInfo) {
        return AjaxResult.success(sysMemberInviteService.listActiveRepoInviteLinks(
                repositoryId, InviteLinkUrlSupport.resolveInviteBaseUrl(uriInfo)));
    }

    @POST
    @Path("/{repositoryId:[0-9]+}/invites")
    @RequiresPermissions(value = {"system:git:repo:manage", "system:git:repo:list", "system:git:repo:pull"},
            logical = RequiresPermissions.Logical.OR)
    public AjaxResult createInvite(@PathParam("repositoryId") Long repositoryId,
                                   SysGitRepoInviteCreateRequest body,
                                   @Context UriInfo uriInfo) {
        String role = body != null ? body.getRole() : null;
        return AjaxResult.success(sysMemberInviteService.createRepoInviteLink(
                repositoryId, role, SecurityUtils.getUserId(),
                InviteLinkUrlSupport.resolveInviteBaseUrl(uriInfo)));
    }

    @POST
    @Path("/{repositoryId:[0-9]+}/invites/{inviteId}/revoke")
    @RequiresPermissions(value = {"system:git:repo:manage", "system:git:repo:list", "system:git:repo:pull"},
            logical = RequiresPermissions.Logical.OR)
    public AjaxResult revokeInvite(@PathParam("repositoryId") Long repositoryId,
                                   @PathParam("inviteId") Long inviteId) {
        return toAjax(sysMemberInviteService.revokeRepoInviteLink(repositoryId, inviteId));
    }

    @GET
    @Path("/{repositoryId:[0-9]+}/invite-sources")
    @RequiresPermissions(value = {"system:git:repo:manage", "system:git:repo:list", "system:git:repo:pull"},
            logical = RequiresPermissions.Logical.OR)
    public AjaxResult listInviteSources(@PathParam("repositoryId") Long repositoryId) {
        return AjaxResult.success(sysGitRepositoryMemberService.listInviteSources(repositoryId));
    }

    @GET
    @Path("/{repositoryId:[0-9]+}/invite-sources/{sourceRepositoryId}/members")
    @RequiresPermissions(value = {"system:git:repo:manage", "system:git:repo:list", "system:git:repo:pull"},
            logical = RequiresPermissions.Logical.OR)
    public AjaxResult listInviteSourceMembers(@PathParam("repositoryId") Long repositoryId,
                                              @PathParam("sourceRepositoryId") Long sourceRepositoryId) {
        return AjaxResult.success(sysGitRepositoryMemberService.listInviteSourceMembers(
                repositoryId, sourceRepositoryId));
    }

    @POST
    @Path("/{repositoryId:[0-9]+}/members/from-repo")
    @RequiresPermissions(value = {"system:git:repo:manage", "system:git:repo:list", "system:git:repo:pull"},
            logical = RequiresPermissions.Logical.OR)
    public AjaxResult addMembersFromRepo(@PathParam("repositoryId") Long repositoryId,
                                         SysGitRepoMembersFromRepoRequest body) {
        Long sourceRepositoryId = body != null ? body.getSourceRepositoryId() : null;
        List<Long> userIds = body != null ? body.getUserIds() : null;
        String role = body != null ? body.getRole() : null;
        GitRepoMembersFromRepoResult result = sysGitRepositoryMemberService.insertMembersFromRepo(
                repositoryId, sourceRepositoryId, userIds, role);
        return AjaxResult.success(result);
    }

    private static AjaxResult toAjax(int rows) {
        return rows > 0 ? AjaxResult.success() : AjaxResult.error("操作失败");
    }

    private static Long[] parseIds(String raw) {
        if (raw == null || raw.isBlank()) {
            return new Long[0];
        }
        String[] parts = raw.split(",");
        Long[] ids = new Long[parts.length];
        for (int i = 0; i < parts.length; i++) {
            ids[i] = Long.valueOf(parts[i].trim());
        }
        return ids;
    }
}
