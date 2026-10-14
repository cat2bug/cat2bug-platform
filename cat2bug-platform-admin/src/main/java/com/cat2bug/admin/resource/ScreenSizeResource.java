package com.cat2bug.admin.resource;

import com.cat2bug.admin.service.SystemExcelExportService;
import com.cat2bug.common.api.AjaxResult;
import com.cat2bug.common.api.TableDataInfo;
import com.cat2bug.common.domain.SysScreenSize;
import com.cat2bug.framework.security.RequiresPermissions;
import com.cat2bug.system.service.SysScreenSizeService;
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

@Path("/system/ScreenSize")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ScreenSizeResource {

    static final String SPREADSHEET_MEDIA_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Inject
    SysScreenSizeService sysScreenSizeService;

    @Inject
    SystemExcelExportService systemExcelExportService;

    @GET
    @Path("/list")
    @RequiresPermissions("system:ScreenSize:list")
    public TableDataInfo list(@QueryParam("name") String name,
                              @QueryParam("pageNum") Integer pageNum,
                              @QueryParam("pageSize") Integer pageSize) {
        SysScreenSize query = new SysScreenSize();
        query.setName(name);
        // 浏览器组件常不传分页；无分页时返回全量
        if (pageNum == null && pageSize == null) {
            var rows = sysScreenSizeService.selectScreenSizeListAll(query);
            return new TableDataInfo(rows, rows.size());
        }
        return sysScreenSizeService.selectScreenSizePage(query, pageNum, pageSize);
    }

    @POST
    @Path("/export")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(SPREADSHEET_MEDIA_TYPE)
    @RequiresPermissions("system:ScreenSize:export")
    public Response exportForm(@RestForm("name") String name) {
        SysScreenSize query = new SysScreenSize();
        query.setName(name);
        return exportSpreadsheet(query);
    }

    @POST
    @Path("/export")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(SPREADSHEET_MEDIA_TYPE)
    @RequiresPermissions("system:ScreenSize:export")
    public Response exportJson(SysScreenSize query) {
        if (query == null) {
            query = new SysScreenSize();
        }
        return exportSpreadsheet(query);
    }

    private Response exportSpreadsheet(SysScreenSize query) {
        byte[] workbook = systemExcelExportService.exportScreenSizes(query);
        return Response.ok(workbook).type(SPREADSHEET_MEDIA_TYPE).build();
    }

    @GET
    @Path("/{screenSizeId}")
    @RequiresPermissions("system:ScreenSize:query")
    public AjaxResult getInfo(@PathParam("screenSizeId") Long screenSizeId) {
        return AjaxResult.success(sysScreenSizeService.selectSysScreenSizeByScreenSizeId(screenSizeId));
    }

    @POST
    @RequiresPermissions("system:ScreenSize:add")
    public AjaxResult add(SysScreenSize sysScreenSize) {
        return toAjax(sysScreenSizeService.insertSysScreenSize(sysScreenSize));
    }

    @PUT
    @RequiresPermissions("system:ScreenSize:edit")
    public AjaxResult edit(SysScreenSize sysScreenSize) {
        return toAjax(sysScreenSizeService.updateSysScreenSize(sysScreenSize));
    }

    @DELETE
    @Path("/{screenSizeIds}")
    @RequiresPermissions("system:ScreenSize:remove")
    public AjaxResult remove(@PathParam("screenSizeIds") String screenSizeIds) {
        Long[] ids = Arrays.stream(screenSizeIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .toArray(Long[]::new);
        return toAjax(sysScreenSizeService.deleteSysScreenSizeByScreenSizeIds(ids));
    }

    private AjaxResult toAjax(int rows) {
        return rows > 0 ? AjaxResult.success() : AjaxResult.error("操作失败");
    }
}
