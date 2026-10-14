package com.cat2bug.admin.resource.open;

import com.cat2bug.common.api.AjaxResult;
import com.cat2bug.system.open.OpenApiContextService;
import com.cat2bug.system.open.OpenApiProjectService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/project")
@Produces(MediaType.APPLICATION_JSON)
public class OpenApiProjectResource {

    @Inject
    OpenApiContextService openApiContextService;

    @Inject
    OpenApiProjectService openApiProjectService;

    @GET
    public AjaxResult getInfo() {
        Long projectId = openApiContextService.requireProjectId();
        return AjaxResult.success(openApiProjectService.buildProjectInfo(projectId));
    }
}
