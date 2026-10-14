package com.cat2bug.admin.resource;

import com.cat2bug.admin.service.SystemExcelExportService;
import com.cat2bug.common.api.AjaxResult;
import com.cat2bug.common.api.TableDataInfo;
import com.cat2bug.common.domain.LoginUser;
import com.cat2bug.common.domain.SysReport;
import com.cat2bug.framework.security.LoginUserResolver;
import com.cat2bug.framework.security.RequiresPermissions;
import com.cat2bug.system.service.MemberFocusService;
import com.cat2bug.system.service.SysReportHistoryService;
import com.cat2bug.system.service.SysReportService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestForm;

import java.util.Arrays;

@Path("/system/report")
@Produces(MediaType.APPLICATION_JSON)
public class ReportResource {

    static final String SPREADSHEET_MEDIA_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private static final String MODULE_NAME = "report";

    @Inject
    SysReportService sysReportService;

    @Inject
    SysReportHistoryService sysReportHistoryService;

    @Inject
    LoginUserResolver loginUserResolver;

    @Inject
    MemberFocusService memberFocusService;

    @Inject
    SystemExcelExportService systemExcelExportService;

    @GET
    @Path("/list")
    @RequiresPermissions("system:report:list")
    public TableDataInfo list(@QueryParam("projectId") Long projectId,
                              @QueryParam("reportTitle") String reportTitle,
                              @QueryParam("reportDataCoder") String reportDataCoder,
                              @QueryParam("createById") Long createById,
                              @QueryParam("pageNum") Integer pageNum,
                              @QueryParam("pageSize") Integer pageSize) {
        SysReport query = new SysReport();
        query.setProjectId(projectId);
        query.setReportTitle(reportTitle);
        query.setReportDataCoder(reportDataCoder);
        query.setCreateById(createById);
        TableDataInfo table = sysReportService.selectReportPage(query, pageNum, pageSize);
        enrichReportFocusList(table);
        return table;
    }

    @POST
    @Path("/export")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(SPREADSHEET_MEDIA_TYPE)
    @RequiresPermissions("system:report:export")
    public Response export(@RestForm("projectId") Long projectId,
                           @RestForm("reportTitle") String reportTitle,
                           @RestForm("reportDataCoder") String reportDataCoder,
                           @RestForm("createById") Long createById) {
        SysReport query = new SysReport();
        query.setProjectId(projectId);
        query.setReportTitle(reportTitle);
        query.setReportDataCoder(reportDataCoder);
        query.setCreateById(createById);
        byte[] workbook = systemExcelExportService.exportReports(query);
        return Response.ok(workbook).type(SPREADSHEET_MEDIA_TYPE).build();
    }

    @GET
    @Path("/{reportId}")
    @RequiresPermissions("system:report:query")
    public AjaxResult getInfo(@Context ContainerRequestContext requestContext,
                              @PathParam("reportId") Long reportId) {
        SysReport sysReport = sysReportService.selectSysReportByReportId(reportId);
        if (sysReport != null) {
            LoginUser loginUser = loginUserResolver.resolve(requestContext);
            memberFocusService.setFocus(MODULE_NAME, reportId, loginUser.getUser());
            sysReport.setFocusList(memberFocusService.getFocusMemberList(MODULE_NAME, reportId));
        }
        return AjaxResult.success(sysReport);
    }

    @GET
    @Path("/{reportId}/history")
    @RequiresPermissions("system:report:query")
    public AjaxResult historyList(@PathParam("reportId") Long reportId) {
        return AjaxResult.success(sysReportHistoryService.selectSysReportHistoryListByReportId(reportId));
    }

    @GET
    @Path("/history/{historyId}")
    @RequiresPermissions("system:report:query")
    public AjaxResult historyInfo(@PathParam("historyId") Long historyId) {
        return AjaxResult.success(sysReportHistoryService.selectSysReportHistoryByHistoryId(historyId));
    }

    @POST
    @Path("/history/{historyId}/restore")
    @Consumes({MediaType.WILDCARD, MediaType.APPLICATION_JSON})
    @RequiresPermissions("system:report:edit")
    public AjaxResult restoreHistory(@PathParam("historyId") Long historyId) {
        return toAjax(sysReportHistoryService.restoreSysReportHistory(historyId));
    }

    @DELETE
    @Path("/history/{historyId}")
    @RequiresPermissions("system:report:edit")
    public AjaxResult removeHistory(@PathParam("historyId") Long historyId) {
        return toAjax(sysReportHistoryService.deleteSysReportHistoryByHistoryId(historyId));
    }

    private void enrichReportFocusList(TableDataInfo table) {
        if (table.getRows() == null) {
            return;
        }
        for (Object row : table.getRows()) {
            if (row instanceof SysReport report) {
                report.setFocusList(memberFocusService.getFocusMemberList(MODULE_NAME, report.getReportId()));
            }
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("system:report:add")
    public AjaxResult add(SysReport sysReport, @Context HttpHeaders headers) {
        sysReport.setReportSource(resolveClientIp(headers));
        return AjaxResult.success(sysReportService.insertSysReport(sysReport));
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("system:report:edit")
    public AjaxResult edit(SysReport sysReport) {
        return toAjax(sysReportService.updateSysReport(sysReport));
    }

    @DELETE
    @Path("/{reportIds}")
    @RequiresPermissions("system:report:remove")
    public AjaxResult remove(@PathParam("reportIds") String reportIds) {
        Long[] ids = Arrays.stream(reportIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .toArray(Long[]::new);
        return toAjax(sysReportService.deleteSysReportByReportIds(ids));
    }

    private AjaxResult toAjax(int rows) {
        return rows > 0 ? AjaxResult.success() : AjaxResult.error("操作失败");
    }

    @POST
    @Path("/close-edit-window")
    @Consumes({MediaType.WILDCARD, MediaType.APPLICATION_JSON})
    public AjaxResult closeEditWindow() {
        sysReportService.closeEditWindow();
        return AjaxResult.success();
    }

    private static String resolveClientIp(HttpHeaders headers) {
        String ip = firstHeader(headers, "X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = firstHeader(headers, "Proxy-Client-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = firstHeader(headers, "WL-Proxy-Client-IP");
        }
        if (ip != null && ip.length() > 15 && ip.contains(",")) {
            ip = ip.substring(0, ip.indexOf(',')).trim();
        }
        return ip == null ? "" : ip;
    }

    private static String firstHeader(HttpHeaders headers, String name) {
        if (headers == null) {
            return null;
        }
        return headers.getHeaderString(name);
    }
}
