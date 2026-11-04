package com.cat2bug.admin.resource;

import com.cat2bug.common.api.AjaxResult;
import com.cat2bug.common.domain.SysTeamPlan;
import com.cat2bug.framework.security.RequiresPermissions;
import com.cat2bug.framework.security.SecurityUtils;
import com.cat2bug.system.service.SysTeamPlanService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/admin/team-plan")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AdminTeamPlanResource {

    @Inject
    SysTeamPlanService sysTeamPlanService;

    @GET
    @Path("/list")
    @RequiresPermissions("admin:team-plan:list")
    public AjaxResult list() {
        return AjaxResult.success(sysTeamPlanService.listAll());
    }

    @GET
    @Path("/{planId}")
    @RequiresPermissions("admin:team-plan:query")
    public AjaxResult get(@PathParam("planId") Long planId) {
        return AjaxResult.success(sysTeamPlanService.getById(planId));
    }

    @POST
    @RequiresPermissions("admin:team-plan:add")
    public AjaxResult add(SysTeamPlan plan) {
        String op = String.valueOf(SecurityUtils.getUserId());
        return AjaxResult.success(sysTeamPlanService.insert(plan, op));
    }

    @PUT
    @RequiresPermissions("admin:team-plan:edit")
    public AjaxResult edit(SysTeamPlan plan) {
        String op = String.valueOf(SecurityUtils.getUserId());
        return toAjax(sysTeamPlanService.update(plan, op));
    }

    private AjaxResult toAjax(int rows) {
        return rows > 0 ? AjaxResult.success() : AjaxResult.error("操作失败");
    }
}
