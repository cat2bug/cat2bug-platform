package com.cat2bug.quarkus.admin.resource;

import com.cat2bug.quarkus.common.api.AjaxResult;
import com.cat2bug.quarkus.common.domain.SysGitPullRequest;
import com.cat2bug.quarkus.common.domain.SysGitPullRequestComment;
import com.cat2bug.quarkus.framework.security.RequiresPermissions;
import com.cat2bug.quarkus.system.git.pull.service.ISysGitPullRequestService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/system/git/repo/{repositoryId:[0-9]+}/pulls")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GitPullRequestResource {

    @Inject
    ISysGitPullRequestService sysGitPullRequestService;

    @GET
    @RequiresPermissions("system:git:repo:pull")
    public AjaxResult list(@PathParam("repositoryId") Long repositoryId,
                           @QueryParam("state") String state) {
        return AjaxResult.success(sysGitPullRequestService.listPullRequests(repositoryId, state));
    }

    @POST
    @RequiresPermissions("system:git:repo:push")
    public AjaxResult create(@PathParam("repositoryId") Long repositoryId, SysGitPullRequest body) {
        return AjaxResult.success(sysGitPullRequestService.createPullRequest(repositoryId, body));
    }

    @GET
    @Path("/{number}")
    @RequiresPermissions("system:git:repo:pull")
    public AjaxResult get(@PathParam("repositoryId") Long repositoryId,
                          @PathParam("number") Integer number) {
        return AjaxResult.success(sysGitPullRequestService.getPullRequest(repositoryId, number));
    }

    @PATCH
    @Path("/{number}")
    public AjaxResult close(@PathParam("repositoryId") Long repositoryId,
                            @PathParam("number") Integer number) {
        sysGitPullRequestService.closePullRequest(repositoryId, number);
        return AjaxResult.success();
    }

    @GET
    @Path("/{number}/commits")
    @RequiresPermissions("system:git:repo:pull")
    public AjaxResult commits(@PathParam("repositoryId") Long repositoryId,
                              @PathParam("number") Integer number) {
        return AjaxResult.success(sysGitPullRequestService.listCommits(repositoryId, number));
    }

    @GET
    @Path("/{number}/files")
    @RequiresPermissions("system:git:repo:pull")
    public AjaxResult files(@PathParam("repositoryId") Long repositoryId,
                            @PathParam("number") Integer number) {
        return AjaxResult.success(sysGitPullRequestService.listFiles(repositoryId, number));
    }

    @GET
    @Path("/{number}/comments")
    @RequiresPermissions("system:git:repo:pull")
    public AjaxResult comments(@PathParam("repositoryId") Long repositoryId,
                               @PathParam("number") Integer number) {
        return AjaxResult.success(sysGitPullRequestService.listComments(repositoryId, number));
    }

    @POST
    @Path("/{number}/comments")
    @RequiresPermissions("system:git:repo:push")
    public AjaxResult createComment(@PathParam("repositoryId") Long repositoryId,
                                    @PathParam("number") Integer number,
                                    SysGitPullRequestComment body) {
        return AjaxResult.success(sysGitPullRequestService.createComment(repositoryId, number, body));
    }

    @POST
    @Path("/{number}/merge")
    @RequiresPermissions("system:git:repo:manage")
    public AjaxResult merge(@PathParam("repositoryId") Long repositoryId,
                            @PathParam("number") Integer number) {
        return AjaxResult.success(sysGitPullRequestService.mergePullRequest(repositoryId, number));
    }
}
