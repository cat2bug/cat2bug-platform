package com.cat2bug.quarkus.admin.resource;

import com.cat2bug.quarkus.admin.service.SystemExcelExportService;
import com.cat2bug.quarkus.common.api.AjaxResult;
import com.cat2bug.quarkus.common.api.TableDataInfo;
import com.cat2bug.quarkus.common.domain.SysOperLog;
import com.cat2bug.quarkus.framework.security.RequiresPermissions;
import com.cat2bug.quarkus.system.service.SysOperLogService;
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
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestForm;

import java.util.Arrays;
import java.util.HashMap;

@Path("/monitor/operlog")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OperlogResource {

    static final String SPREADSHEET_MEDIA_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Inject
    SysOperLogService sysOperLogService;

    @Inject
    SystemExcelExportService systemExcelExportService;

    @GET
    @Path("/list")
    @RequiresPermissions("monitor:operlog:list")
    public TableDataInfo list(@QueryParam("operIp") String operIp,
                              @QueryParam("title") String title,
                              @QueryParam("operName") String operName,
                              @QueryParam("businessType") Integer businessType,
                              @QueryParam("status") Integer status,
                              @QueryParam("params[beginTime]") String beginTime,
                              @QueryParam("params[endTime]") String endTime,
                              @QueryParam("pageNum") Integer pageNum,
                              @QueryParam("pageSize") Integer pageSize) {
        return sysOperLogService.selectOperLogPage(
                buildQuery(operIp, title, operName, businessType, status, beginTime, endTime),
                pageNum, pageSize);
    }

    @POST
    @Path("/export")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(SPREADSHEET_MEDIA_TYPE)
    @RequiresPermissions("monitor:operlog:export")
    public Response exportForm(@RestForm("operIp") String operIp,
                               @RestForm("title") String title,
                               @RestForm("operName") String operName,
                               @RestForm("businessType") Integer businessType,
                               @RestForm("status") Integer status,
                               @RestForm("params[beginTime]") String beginTime,
                               @RestForm("params[endTime]") String endTime) {
        return exportSpreadsheet(buildQuery(operIp, title, operName, businessType, status, beginTime, endTime));
    }

    @POST
    @Path("/export")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(SPREADSHEET_MEDIA_TYPE)
    @RequiresPermissions("monitor:operlog:export")
    public Response exportJson(SysOperLog query) {
        return exportSpreadsheet(normalizeQuery(query));
    }

    private Response exportSpreadsheet(SysOperLog query) {
        byte[] workbook = systemExcelExportService.exportOperLogs(query);
        return Response.ok(workbook).type(SPREADSHEET_MEDIA_TYPE).build();
    }

    @DELETE
    @Path("/{operIds}")
    @RequiresPermissions("monitor:operlog:remove")
    public AjaxResult remove(@PathParam("operIds") String operIds) {
        Long[] ids = Arrays.stream(operIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .toArray(Long[]::new);
        return toAjax(sysOperLogService.deleteOperLogByIds(ids));
    }

    @DELETE
    @Path("/clean")
    @RequiresPermissions("monitor:operlog:remove")
    public AjaxResult clean() {
        sysOperLogService.cleanOperLog();
        return AjaxResult.success();
    }

    private static SysOperLog buildQuery(String operIp, String title, String operName,
                                         Integer businessType, Integer status,
                                         String beginTime, String endTime) {
        SysOperLog query = new SysOperLog();
        query.setOperIp(operIp);
        query.setTitle(title);
        query.setOperName(operName);
        query.setBusinessType(businessType);
        query.setStatus(status);
        query.setParams(new HashMap<>());
        if (beginTime != null) {
            query.getParams().put("beginTime", beginTime);
        }
        if (endTime != null) {
            query.getParams().put("endTime", endTime);
        }
        return query;
    }

    private static SysOperLog normalizeQuery(SysOperLog query) {
        if (query == null) {
            query = new SysOperLog();
        }
        if (query.getParams() == null) {
            query.setParams(new HashMap<>());
        }
        return query;
    }

    private AjaxResult toAjax(int rows) {
        return rows > 0 ? AjaxResult.success() : AjaxResult.error("操作失败");
    }
}
