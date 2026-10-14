package com.cat2bug.admin.resource;

import com.cat2bug.admin.service.CloudCaseService;
import com.cat2bug.common.api.AjaxResult;
import com.cat2bug.framework.security.RequiresPermissions;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/cloud/api/case")
@Produces(MediaType.APPLICATION_JSON)
public class CloudCaseResource {

    @Inject
    CloudCaseService cloudCaseService;

    @GET
    @Path("/list")
    @RequiresPermissions("system:case:list")
    public AjaxResult list(@QueryParam("content") String content) {
        return AjaxResult.success(cloudCaseService.searchCaseListOfAi(content));
    }
}
