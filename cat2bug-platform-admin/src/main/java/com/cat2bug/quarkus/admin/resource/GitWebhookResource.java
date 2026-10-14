package com.cat2bug.quarkus.admin.resource;

import com.cat2bug.quarkus.common.api.AjaxResult;
import com.cat2bug.quarkus.common.domain.git.SysGitRepositoryWebhookRequest;
import com.cat2bug.quarkus.framework.security.RequiresPermissions;
import com.cat2bug.quarkus.system.service.ISysGitRepositoryWebhookService;
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

@Path("/system/git/repo/{repositoryId:[0-9]+}/webhooks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GitWebhookResource {

    @Inject
    ISysGitRepositoryWebhookService webhookService;

    @GET
    @RequiresPermissions(value = {"system:git:repo:manage", "system:git:repo:list", "system:git:repo:pull"},
            logical = RequiresPermissions.Logical.OR)
    public AjaxResult list(@PathParam("repositoryId") Long repositoryId) {
        return AjaxResult.success(webhookService.listWebhooks(repositoryId));
    }

    @GET
    @Path("/{webhookId}")
    @RequiresPermissions(value = {"system:git:repo:manage", "system:git:repo:list", "system:git:repo:pull"},
            logical = RequiresPermissions.Logical.OR)
    public AjaxResult get(@PathParam("repositoryId") Long repositoryId,
                          @PathParam("webhookId") Long webhookId) {
        return AjaxResult.success(webhookService.getWebhook(repositoryId, webhookId));
    }

    @POST
    @RequiresPermissions(value = {"system:git:repo:manage", "system:git:repo:list", "system:git:repo:pull"},
            logical = RequiresPermissions.Logical.OR)
    public AjaxResult create(@PathParam("repositoryId") Long repositoryId,
                             SysGitRepositoryWebhookRequest body) {
        return AjaxResult.success(webhookService.createWebhook(repositoryId, body));
    }

    @PUT
    @Path("/{webhookId}")
    @RequiresPermissions(value = {"system:git:repo:manage", "system:git:repo:list", "system:git:repo:pull"},
            logical = RequiresPermissions.Logical.OR)
    public AjaxResult update(@PathParam("repositoryId") Long repositoryId,
                             @PathParam("webhookId") Long webhookId,
                             SysGitRepositoryWebhookRequest body) {
        return AjaxResult.success(webhookService.updateWebhook(repositoryId, webhookId, body));
    }

    @DELETE
    @Path("/{webhookId}")
    @RequiresPermissions(value = {"system:git:repo:manage", "system:git:repo:list", "system:git:repo:pull"},
            logical = RequiresPermissions.Logical.OR)
    public AjaxResult delete(@PathParam("repositoryId") Long repositoryId,
                             @PathParam("webhookId") Long webhookId) {
        return webhookService.deleteWebhook(repositoryId, webhookId) > 0
                ? AjaxResult.success() : AjaxResult.error("操作失败");
    }

    @POST
    @Path("/{webhookId}/secret/regenerate")
    @RequiresPermissions(value = {"system:git:repo:manage", "system:git:repo:list", "system:git:repo:pull"},
            logical = RequiresPermissions.Logical.OR)
    public AjaxResult regenerateSecret(@PathParam("repositoryId") Long repositoryId,
                                       @PathParam("webhookId") Long webhookId) {
        return AjaxResult.success(webhookService.regenerateSecret(repositoryId, webhookId));
    }

    @GET
    @Path("/{webhookId}/deliveries")
    @RequiresPermissions(value = {"system:git:repo:manage", "system:git:repo:list", "system:git:repo:pull"},
            logical = RequiresPermissions.Logical.OR)
    public AjaxResult listDeliveries(@PathParam("repositoryId") Long repositoryId,
                                     @PathParam("webhookId") Long webhookId) {
        return AjaxResult.success(webhookService.listDeliveries(repositoryId, webhookId));
    }

    @POST
    @Path("/{webhookId}/test")
    @RequiresPermissions(value = {"system:git:repo:manage", "system:git:repo:list", "system:git:repo:pull"},
            logical = RequiresPermissions.Logical.OR)
    public AjaxResult test(@PathParam("repositoryId") Long repositoryId,
                           @PathParam("webhookId") Long webhookId) {
        webhookService.sendTestPing(repositoryId, webhookId);
        return AjaxResult.success();
    }
}
