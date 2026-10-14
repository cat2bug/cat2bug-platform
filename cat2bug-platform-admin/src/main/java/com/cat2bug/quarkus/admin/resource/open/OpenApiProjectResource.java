package com.cat2bug.quarkus.admin.resource.open;

import com.cat2bug.quarkus.common.api.AjaxResult;
import com.cat2bug.quarkus.system.open.OpenApiContextService;
import com.cat2bug.quarkus.system.open.OpenApiProjectService;
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
