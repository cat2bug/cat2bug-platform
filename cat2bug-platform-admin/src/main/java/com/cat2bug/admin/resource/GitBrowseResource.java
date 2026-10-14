package com.cat2bug.admin.resource;

import com.cat2bug.admin.support.JaxrsBinaryHttpResponse;
import com.cat2bug.common.api.AjaxResult;
import com.cat2bug.framework.security.RequiresPermissions;
import com.cat2bug.system.git.browse.domain.GitBranchCreateRequest;
import com.cat2bug.system.git.browse.domain.GitBranchNameRequest;
import com.cat2bug.system.git.browse.domain.GitTagCreateRequest;
import com.cat2bug.system.git.browse.service.ISysGitBrowseService;
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
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import java.io.IOException;

@Path("/system/git/repo")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GitBrowseResource {

    @Inject
    ISysGitBrowseService gitBrowseService;

    @GET
    @Path("/{repositoryId:[0-9]+}/branches")
    @RequiresPermissions("system:git:repo:pull")
    public AjaxResult branches(@PathParam("repositoryId") Long repositoryId,
                               @QueryParam("name") String name) {
        return AjaxResult.success(gitBrowseService.listBranches(repositoryId, name));
    }

    @POST
    @Path("/{repositoryId:[0-9]+}/branches")
    @RequiresPermissions("system:git:repo:manage")
    public AjaxResult createBranch(@PathParam("repositoryId") Long repositoryId,
                                   GitBranchCreateRequest body) {
        return AjaxResult.success(gitBrowseService.createBranch(
                repositoryId, body.getName(), body.getFromRef()));
    }

    @DELETE
    @Path("/{repositoryId:[0-9]+}/branches/{name}")
    @RequiresPermissions("system:git:repo:manage")
    public AjaxResult deleteBranch(@PathParam("repositoryId") Long repositoryId,
                                   @PathParam("name") String name) {
        gitBrowseService.deleteBranch(repositoryId, name);
        return AjaxResult.success();
    }

    @PUT
    @Path("/{repositoryId:[0-9]+}/default-branch")
    @RequiresPermissions("system:git:repo:manage")
    public AjaxResult setDefaultBranch(@PathParam("repositoryId") Long repositoryId,
                                       GitBranchNameRequest body) {
        gitBrowseService.setDefaultBranch(repositoryId, body.getName());
        return AjaxResult.success();
    }

    @GET
    @Path("/{repositoryId:[0-9]+}/tags")
    @RequiresPermissions("system:git:repo:pull")
    public AjaxResult tags(@PathParam("repositoryId") Long repositoryId,
                           @QueryParam("q") String q) {
        return AjaxResult.success(gitBrowseService.listTags(repositoryId, q));
    }

    @POST
    @Path("/{repositoryId:[0-9]+}/tags")
    @RequiresPermissions("system:git:repo:manage")
    public AjaxResult createTag(@PathParam("repositoryId") Long repositoryId,
                                GitTagCreateRequest body) {
        return AjaxResult.success(gitBrowseService.createTag(
                repositoryId, body.getName(), body.getMessage(),
                body.getTargetRef(), body.getTargetCommitId()));
    }

    @DELETE
    @Path("/{repositoryId:[0-9]+}/tags/{name}")
    @RequiresPermissions("system:git:repo:manage")
    public AjaxResult deleteTag(@PathParam("repositoryId") Long repositoryId,
                                @PathParam("name") String name,
                                @QueryParam("cascadeRelease") Boolean cascadeRelease) {
        gitBrowseService.deleteTag(repositoryId, name, cascadeRelease);
        return AjaxResult.success();
    }

    @GET
    @Path("/{repositoryId:[0-9]+}/tree")
    @RequiresPermissions("system:git:repo:pull")
    public AjaxResult tree(@PathParam("repositoryId") Long repositoryId,
                           @QueryParam("ref") String ref,
                           @QueryParam("path") String path) {
        return AjaxResult.success(gitBrowseService.listTree(repositoryId, ref, path));
    }

    @GET
    @Path("/{repositoryId:[0-9]+}/blob")
    @RequiresPermissions("system:git:repo:pull")
    public AjaxResult blob(@PathParam("repositoryId") Long repositoryId,
                           @QueryParam("ref") String ref,
                           @QueryParam("path") String path) {
        return AjaxResult.success(gitBrowseService.readBlob(repositoryId, ref, path));
    }

    @GET
    @Path("/{repositoryId:[0-9]+}/raw")
    @RequiresPermissions("system:git:repo:pull")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response raw(@PathParam("repositoryId") Long repositoryId,
                        @QueryParam("ref") String ref,
                        @QueryParam("path") String path) throws IOException {
        JaxrsBinaryHttpResponse binary = new JaxrsBinaryHttpResponse();
        gitBrowseService.writeRawBlob(repositoryId, ref, path, binary);
        return buildBinary(binary);
    }

    @GET
    @Path("/{repositoryId:[0-9]+}/commit-count")
    @RequiresPermissions("system:git:repo:pull")
    public AjaxResult commitCount(@PathParam("repositoryId") Long repositoryId,
                                  @QueryParam("ref") String ref) {
        return AjaxResult.success(gitBrowseService.countCommits(repositoryId, ref));
    }

    @GET
    @Path("/{repositoryId:[0-9]+}/commits/{commitId}")
    @RequiresPermissions("system:git:repo:pull")
    public AjaxResult commitDetail(@PathParam("repositoryId") Long repositoryId,
                                   @PathParam("commitId") String commitId) {
        return AjaxResult.success(gitBrowseService.getCommitDetail(repositoryId, commitId));
    }

    @GET
    @Path("/{repositoryId:[0-9]+}/commits")
    @RequiresPermissions("system:git:repo:pull")
    public AjaxResult commits(@PathParam("repositoryId") Long repositoryId,
                              @QueryParam("ref") String ref,
                              @QueryParam("limit") Integer limit,
                              @QueryParam("author") String author,
                              @QueryParam("since") String since,
                              @QueryParam("until") String until) {
        return AjaxResult.success(gitBrowseService.listCommits(
                repositoryId, ref, limit, author, since, until));
    }

    @GET
    @Path("/{repositoryId:[0-9]+}/contributors")
    @RequiresPermissions("system:git:repo:pull")
    public AjaxResult contributors(@PathParam("repositoryId") Long repositoryId,
                                   @QueryParam("ref") String ref,
                                   @QueryParam("limit") Integer limit) {
        return AjaxResult.success(gitBrowseService.listContributors(repositoryId, ref, limit));
    }

    @GET
    @Path("/{repositoryId:[0-9]+}/archive")
    @RequiresPermissions("system:git:repo:pull")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response archive(@PathParam("repositoryId") Long repositoryId,
                            @QueryParam("ref") String ref,
                            @QueryParam("format") @DefaultValue("zip") String format) throws IOException {
        JaxrsBinaryHttpResponse binary = new JaxrsBinaryHttpResponse();
        gitBrowseService.writeArchive(repositoryId, ref, format, binary);
        return buildBinary(binary);
    }

    private static Response buildBinary(JaxrsBinaryHttpResponse binary) {
        Response.ResponseBuilder builder = Response.ok(binary.toByteArray()).type(binary.getContentType());
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        binary.applyHeaders(headers);
        for (var e : headers.entrySet()) {
            for (Object v : e.getValue()) {
                builder.header(e.getKey(), v);
            }
        }
        return builder.build();
    }
}
