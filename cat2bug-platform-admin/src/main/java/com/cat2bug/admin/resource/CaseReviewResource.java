package com.cat2bug.admin.resource;

import com.cat2bug.common.api.AjaxResult;
import com.cat2bug.common.domain.SysCaseReview;
import com.cat2bug.common.domain.SysCaseReviewItem;
import com.cat2bug.framework.security.RequiresPermissions;
import com.cat2bug.system.service.SysCaseReviewService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/system/case-review")
@Produces(MediaType.APPLICATION_JSON)
public class CaseReviewResource {

    @Inject
    SysCaseReviewService sysCaseReviewService;

    @GET
    @RequiresPermissions("system:case:query")
    public AjaxResult list(@QueryParam("projectId") Long projectId) {
        return AjaxResult.success(sysCaseReviewService.list(projectId));
    }

    @GET
    @Path("/{reviewId}")
    @RequiresPermissions("system:case:query")
    public AjaxResult get(@PathParam("reviewId") Long reviewId) {
        return AjaxResult.success(sysCaseReviewService.get(reviewId));
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("system:case:review")
    public AjaxResult start(SysCaseReview body) {
        if (body == null) {
            body = new SysCaseReview();
        }
        return AjaxResult.success(sysCaseReviewService.start(body.getProjectId(), body.getCaseIds()));
    }

    @POST
    @Path("/submit")
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("system:case:review")
    public AjaxResult submit(SysCaseReview body) {
        if (body == null) {
            body = new SysCaseReview();
        }
        return AjaxResult.success(sysCaseReviewService.submit(body.getProjectId(), body.getItems()));
    }

    @PUT
    @Path("/{reviewId}/item")
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("system:case:review")
    public AjaxResult comment(@PathParam("reviewId") Long reviewId, SysCaseReviewItem item) {
        if (item == null) {
            item = new SysCaseReviewItem();
        }
        return AjaxResult.success(sysCaseReviewService.comment(reviewId, item.getCaseId(), item.getVerdict(), item.getComment()));
    }

    @POST
    @Path("/{reviewId}/complete")
    @RequiresPermissions("system:case:review")
    public AjaxResult complete(@PathParam("reviewId") Long reviewId) {
        return AjaxResult.success(sysCaseReviewService.complete(reviewId));
    }

    @PUT
    @Path("/{reviewId}/report")
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("system:case:review")
    public AjaxResult report(@PathParam("reviewId") Long reviewId, SysCaseReview body) {
        return AjaxResult.success(sysCaseReviewService.updateReport(reviewId, body == null ? null : body.getReportMarkdown()));
    }
}
