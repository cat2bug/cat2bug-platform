package com.cat2bug.admin.resource;

import com.cat2bug.admin.support.JaxrsBinaryHttpResponse;
import com.cat2bug.common.api.AjaxResult;
import com.cat2bug.common.domain.SysGitRelease;
import com.cat2bug.framework.security.RequiresPermissions;
import com.cat2bug.system.git.release.service.ISysGitReleaseService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.IOException;
import java.nio.file.Files;

@Path("/system/git/repo/{repositoryId:[0-9]+}/releases")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GitReleaseResource {

    @Inject
    ISysGitReleaseService sysGitReleaseService;

    @GET
    @RequiresPermissions("system:git:repo:pull")
    public AjaxResult list(@PathParam("repositoryId") Long repositoryId) {
        return AjaxResult.success(sysGitReleaseService.listReleases(repositoryId));
    }

    @GET
    @Path("/{releaseId}")
    @RequiresPermissions("system:git:repo:pull")
    public AjaxResult get(@PathParam("repositoryId") Long repositoryId,
                          @PathParam("releaseId") Long releaseId) {
        return AjaxResult.success(sysGitReleaseService.getRelease(repositoryId, releaseId));
    }

    @POST
    @RequiresPermissions("system:git:repo:manage")
    public AjaxResult create(@PathParam("repositoryId") Long repositoryId, SysGitRelease body) {
        return AjaxResult.success(sysGitReleaseService.createRelease(repositoryId, body));
    }

    @PUT
    @Path("/{releaseId}")
    @RequiresPermissions("system:git:repo:manage")
    public AjaxResult update(@PathParam("repositoryId") Long repositoryId,
                             @PathParam("releaseId") Long releaseId,
                             SysGitRelease body) {
        return AjaxResult.success(sysGitReleaseService.updateRelease(repositoryId, releaseId, body));
    }

    @DELETE
    @Path("/{releaseId}")
    @RequiresPermissions("system:git:repo:manage")
    public AjaxResult remove(@PathParam("repositoryId") Long repositoryId,
                             @PathParam("releaseId") Long releaseId) {
        sysGitReleaseService.deleteRelease(repositoryId, releaseId);
        return AjaxResult.success();
    }

    @POST
    @Path("/{releaseId}/assets")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @RequiresPermissions("system:git:repo:manage")
    public AjaxResult uploadAsset(@PathParam("repositoryId") Long repositoryId,
                                  @PathParam("releaseId") Long releaseId,
                                  @RestForm("file") FileUpload file) throws IOException {
        long size = file != null && file.uploadedFile() != null ? Files.size(file.uploadedFile()) : 0L;
        return AjaxResult.success(sysGitReleaseService.uploadAsset(
                repositoryId, releaseId,
                file != null ? file.uploadedFile() : null,
                file != null ? file.fileName() : null,
                file != null ? file.contentType() : null,
                size));
    }

    @GET
    @Path("/{releaseId}/assets")
    @RequiresPermissions("system:git:repo:pull")
    public AjaxResult listAssets(@PathParam("repositoryId") Long repositoryId,
                                 @PathParam("releaseId") Long releaseId) {
        return AjaxResult.success(sysGitReleaseService.listAssets(repositoryId, releaseId));
    }

    @DELETE
    @Path("/{releaseId}/assets/{assetId}")
    @RequiresPermissions("system:git:repo:manage")
    public AjaxResult deleteAsset(@PathParam("repositoryId") Long repositoryId,
                                  @PathParam("releaseId") Long releaseId,
                                  @PathParam("assetId") Long assetId) {
        sysGitReleaseService.deleteAsset(repositoryId, releaseId, assetId);
        return AjaxResult.success();
    }

    @GET
    @Path("/{releaseId}/assets/{assetId}/download")
    @RequiresPermissions("system:git:repo:pull")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadAsset(@PathParam("repositoryId") Long repositoryId,
                                  @PathParam("releaseId") Long releaseId,
                                  @PathParam("assetId") Long assetId) throws IOException {
        JaxrsBinaryHttpResponse binary = new JaxrsBinaryHttpResponse();
        sysGitReleaseService.downloadAsset(repositoryId, releaseId, assetId, binary);
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
