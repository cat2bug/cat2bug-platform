package com.cat2bug.admin.resource;

import com.cat2bug.common.api.AjaxResult;
import com.cat2bug.common.domain.SysDefectPlanBaseline;
import com.cat2bug.common.domain.SysDefectPlanLink;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.framework.security.RequiresPermissions;
import com.cat2bug.system.service.SysDefectPlanBaselineService;
import com.cat2bug.system.service.SysDefectPlanLinkService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 缺陷甘特二期：计划依赖与基线。
 * 路径挂在 /system/defect/plan-*，避免与 /{defectId} 冲突。
 */
@Path("/system/defect")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DefectPlanGanttResource {

    @Inject
    SysDefectPlanLinkService planLinkService;

    @Inject
    SysDefectPlanBaselineService planBaselineService;

    @GET
    @Path("/plan-link")
    @RequiresPermissions("system:defect:list")
    public AjaxResult listLinks(@QueryParam("projectId") Long projectId,
                                @QueryParam("defectIds") String defectIdsCsv) {
        List<Long> ids = parseIds(defectIdsCsv);
        if (ids.isEmpty()) {
            return AjaxResult.success(planLinkService.listByProject(projectId));
        }
        return AjaxResult.success(planLinkService.listByProjectAndDefectIds(projectId, ids));
    }

    @POST
    @Path("/plan-link")
    @RequiresPermissions("system:defect:edit")
    public AjaxResult createLink(SysDefectPlanLink body) {
        return AjaxResult.success(planLinkService.create(body));
    }

    @DELETE
    @Path("/plan-link/{linkId}")
    @RequiresPermissions("system:defect:edit")
    public AjaxResult deleteLink(@PathParam("linkId") Long linkId) {
        planLinkService.delete(linkId);
        return AjaxResult.success();
    }

    @GET
    @Path("/plan-baseline")
    @RequiresPermissions("system:defect:list")
    public AjaxResult getBaseline(@QueryParam("projectId") Long projectId,
                                  @QueryParam("defectIds") String defectIdsCsv) {
        List<Long> ids = parseIds(defectIdsCsv);
        SysDefectPlanBaseline baseline = ids.isEmpty()
                ? planBaselineService.getActive(projectId)
                : planBaselineService.getActiveForDefectIds(projectId, ids);
        return AjaxResult.success(baseline);
    }

    @POST
    @Path("/plan-baseline/freeze")
    @RequiresPermissions("system:defect:edit")
    public AjaxResult freezeBaseline(Map<String, Object> body) {
        Long projectId = readLong(body, "projectId");
        if (projectId == null) {
            throw new ApiException(400, "项目ID不能为空");
        }
        return AjaxResult.success(planBaselineService.freeze(projectId));
    }

    @DELETE
    @Path("/plan-baseline")
    @RequiresPermissions("system:defect:edit")
    public AjaxResult clearBaseline(@QueryParam("projectId") Long projectId) {
        planBaselineService.clear(projectId);
        return AjaxResult.success();
    }

    /** 甘特配套：一次取依赖边 + 基线（按缺陷 ID 过滤）。 */
    @GET
    @Path("/plan-gantt-meta")
    @RequiresPermissions("system:defect:list")
    public AjaxResult ganttMeta(@QueryParam("projectId") Long projectId,
                                @QueryParam("defectIds") String defectIdsCsv) {
        List<Long> ids = parseIds(defectIdsCsv);
        Map<String, Object> data = new HashMap<>();
        data.put("links", ids.isEmpty()
                ? planLinkService.listByProject(projectId)
                : planLinkService.listByProjectAndDefectIds(projectId, ids));
        data.put("baseline", ids.isEmpty()
                ? planBaselineService.getActive(projectId)
                : planBaselineService.getActiveForDefectIds(projectId, ids));
        return AjaxResult.success(data);
    }

    private static List<Long> parseIds(String csv) {
        List<Long> out = new ArrayList<>();
        if (csv == null || csv.isBlank()) {
            return out;
        }
        for (String part : csv.split(",")) {
            if (part == null || part.isBlank()) continue;
            try {
                out.add(Long.parseLong(part.trim()));
            } catch (NumberFormatException ignored) {
                // skip
            }
        }
        return out;
    }

    private static Long readLong(Map<String, Object> body, String key) {
        if (body == null || key == null) return null;
        Object v = body.get(key);
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).longValue();
        try {
            return Long.parseLong(String.valueOf(v));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
