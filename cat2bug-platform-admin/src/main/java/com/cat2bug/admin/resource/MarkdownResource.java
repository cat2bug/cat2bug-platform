package com.cat2bug.admin.resource;

import com.cat2bug.common.api.AjaxResult;
import com.cat2bug.framework.security.RequiresPermissions;
import com.cat2bug.system.service.MarkdownPluginService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/markdown")
@Produces(MediaType.APPLICATION_JSON)
public class MarkdownResource {

    @Inject
    MarkdownPluginService markdownPluginService;

    @GET
    @Path("/{projectId}")
    @RequiresPermissions("system:report:list")
    public AjaxResult initData(@PathParam("projectId") Long projectId) {
        return AjaxResult.success(markdownPluginService.initProjectData(projectId));
    }
}
