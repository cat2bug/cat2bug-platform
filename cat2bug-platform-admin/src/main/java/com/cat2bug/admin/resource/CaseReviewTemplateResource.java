package com.cat2bug.admin.resource;

import com.cat2bug.common.api.AjaxResult;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.framework.security.RequiresPermissions;
import com.cat2bug.system.service.SysCaseTypeService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.Map;

@Path("/system/project/{projectId}/case-review-template")
@Produces(MediaType.APPLICATION_JSON)
public class CaseReviewTemplateResource {

    private static final int HTTP_GONE = 410;
    private static final String MIGRATED_MSG =
            "评审纪要模板已迁至团队设置，请使用 /system/team/{teamId}/case-review-template";

    @Inject
    SysCaseTypeService sysCaseTypeService;

    @GET
    @RequiresPermissions("system:project:case-type:list")
    public AjaxResult get(@PathParam("projectId") Long projectId) {
        String template = sysCaseTypeService.reviewTemplate(projectId);
        return AjaxResult.success(Map.of("template", template == null ? "" : template));
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("system:project:case-type:edit")
    public AjaxResult save(@PathParam("projectId") Long projectId, Map<String, String> body) {
        throw new ApiException(HTTP_GONE, MIGRATED_MSG);
    }
}
