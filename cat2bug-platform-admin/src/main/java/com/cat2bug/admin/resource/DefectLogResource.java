package com.cat2bug.admin.resource;

import com.cat2bug.admin.service.SystemExcelExportService;
import com.cat2bug.common.api.AjaxResult;
import com.cat2bug.common.api.TableDataInfo;
import com.cat2bug.common.constant.DefectLogState;
import com.cat2bug.common.domain.SysDefectLog;
import com.cat2bug.framework.security.RequiresPermissions;
import com.cat2bug.system.service.SysDefectLogService;
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
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestForm;

import java.util.Arrays;

@Path("/system/log")
@Produces(MediaType.APPLICATION_JSON)
public class DefectLogResource {

    static final String SPREADSHEET_MEDIA_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Inject
    SysDefectLogService sysDefectLogService;

    @Inject
    SystemExcelExportService systemExcelExportService;

    @GET
    @Path("/list")
    @RequiresPermissions(value = {"system:defect:list", "system:defect:log:list"},
            logical = RequiresPermissions.Logical.OR)
    public TableDataInfo list(@QueryParam("defectId") Long defectId,
                              @QueryParam("defectLogType") DefectLogState defectLogType,
                              @QueryParam("pageNum") Integer pageNum,
                              @QueryParam("pageSize") Integer pageSize) {
        SysDefectLog query = new SysDefectLog();
        query.setDefectId(defectId);
        query.setDefectLogType(defectLogType);
        return sysDefectLogService.selectLogPage(query, pageNum, pageSize);
    }

    @POST
    @Path("/export")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(SPREADSHEET_MEDIA_TYPE)
    @RequiresPermissions("system:defect:log:export")
    public Response export(@RestForm("defectId") Long defectId,
                           @RestForm("defectLogType") DefectLogState defectLogType) {
        SysDefectLog query = new SysDefectLog();
        query.setDefectId(defectId);
        query.setDefectLogType(defectLogType);
        byte[] workbook = systemExcelExportService.exportDefectLogs(query);
        return Response.ok(workbook).type(SPREADSHEET_MEDIA_TYPE).build();
    }

    @GET
    @Path("/{defectLogId}")
    @RequiresPermissions(value = {"system:defect:list", "system:defect:log:query"},
            logical = RequiresPermissions.Logical.OR)
    public AjaxResult getInfo(@PathParam("defectLogId") Long defectLogId) {
        return AjaxResult.success(sysDefectLogService.selectSysDefectLogByDefectLogId(defectLogId));
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("system:defect:log:add")
    public AjaxResult add(SysDefectLog sysDefectLog) {
        return toAjax(sysDefectLogService.insertSysDefectLog(sysDefectLog));
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("system:defect:log:edit")
    public AjaxResult edit(SysDefectLog sysDefectLog) {
        return toAjax(sysDefectLogService.updateSysDefectLog(sysDefectLog));
    }

    @DELETE
    @Path("/{defectLogIds}")
    @RequiresPermissions("system:defect:log:remove")
    public AjaxResult remove(@PathParam("defectLogIds") String defectLogIds) {
        Long[] ids = Arrays.stream(defectLogIds.split(","))
                .map(String::trim).filter(v -> !v.isEmpty()).map(Long::valueOf).toArray(Long[]::new);
        return toAjax(sysDefectLogService.deleteSysDefectLogByDefectLogIds(ids));
    }

    private static AjaxResult toAjax(int rows) {
        return rows > 0 ? AjaxResult.success() : AjaxResult.error("操作失败");
    }
}
