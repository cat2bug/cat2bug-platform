package com.cat2bug.admin.resource;

import com.cat2bug.common.api.AjaxResult;
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

@Path("/system/team/{teamId}/case-review-template")
@Produces(MediaType.APPLICATION_JSON)
public class TeamCaseReviewTemplateResource {

    @Inject
    SysCaseTypeService sysCaseTypeService;

    @GET
    @RequiresPermissions("system:team:case-type:list")
    public AjaxResult get(@PathParam("teamId") Long teamId) {
        String template = sysCaseTypeService.reviewTemplateByTeam(teamId);
        return AjaxResult.success(Map.of("template", template == null ? "" : template));
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("system:team:case-type:edit")
    public AjaxResult save(@PathParam("teamId") Long teamId, Map<String, String> body) {
        sysCaseTypeService.saveReviewTemplateByTeam(teamId, body == null ? null : body.get("template"));
        return AjaxResult.success();
    }
}
