package com.cat2bug.admin.resource;

import com.cat2bug.common.api.AjaxResult;
import com.cat2bug.framework.security.RequiresPermissions;
import com.cat2bug.system.service.SysDefectPageSettingsService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/system/project/{projectId}/defect-page-settings")
@Produces(MediaType.APPLICATION_JSON)
public class ProjectDefectPageSettingsResource {

    @Inject
    SysDefectPageSettingsService sysDefectPageSettingsService;

    @GET
    @RequiresPermissions("system:defect:list")
    public AjaxResult get(@PathParam("projectId") Long projectId) {
        return AjaxResult.success(sysDefectPageSettingsService.getByProjectId(projectId));
    }
}
