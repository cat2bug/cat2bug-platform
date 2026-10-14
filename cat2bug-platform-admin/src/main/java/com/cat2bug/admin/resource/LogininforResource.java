package com.cat2bug.admin.resource;

import com.cat2bug.admin.service.SystemExcelExportService;
import com.cat2bug.common.api.AjaxResult;
import com.cat2bug.common.api.TableDataInfo;
import com.cat2bug.common.domain.SysLogininfor;
import com.cat2bug.framework.security.RequiresPermissions;
import com.cat2bug.system.service.SysLogininforService;
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

@Path("/monitor/logininfor")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LogininforResource {

    static final String SPREADSHEET_MEDIA_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Inject
    SysLogininforService sysLogininforService;

    @Inject
    SystemExcelExportService systemExcelExportService;

    @GET
    @Path("/list")
    @RequiresPermissions("monitor:logininfor:list")
    public TableDataInfo list(@QueryParam("ipaddr") String ipaddr,
                              @QueryParam("userName") String userName,
                              @QueryParam("status") String status,
                              @QueryParam("params[beginTime]") String beginTime,
                              @QueryParam("params[endTime]") String endTime,
                              @QueryParam("pageNum") Integer pageNum,
                              @QueryParam("pageSize") Integer pageSize) {
        return sysLogininforService.selectLogininforPage(
                buildQuery(ipaddr, userName, status, beginTime, endTime),
                pageNum, pageSize);
    }

    @POST
    @Path("/export")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(SPREADSHEET_MEDIA_TYPE)
    @RequiresPermissions("monitor:logininfor:export")
    public Response exportForm(@RestForm("ipaddr") String ipaddr,
                               @RestForm("userName") String userName,
                               @RestForm("status") String status,
                               @RestForm("params[beginTime]") String beginTime,
                               @RestForm("params[endTime]") String endTime) {
        return exportSpreadsheet(buildQuery(ipaddr, userName, status, beginTime, endTime));
    }

    @POST
    @Path("/export")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(SPREADSHEET_MEDIA_TYPE)
    @RequiresPermissions("monitor:logininfor:export")
    public Response exportJson(SysLogininfor query) {
        return exportSpreadsheet(normalizeQuery(query));
    }

    private Response exportSpreadsheet(SysLogininfor query) {
        byte[] workbook = systemExcelExportService.exportLogininfor(query);
        return Response.ok(workbook).type(SPREADSHEET_MEDIA_TYPE).build();
    }

    @DELETE
    @Path("/{infoIds}")
    @RequiresPermissions("monitor:logininfor:remove")
    public AjaxResult remove(@PathParam("infoIds") String infoIds) {
        Long[] ids = Arrays.stream(infoIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .toArray(Long[]::new);
        return toAjax(sysLogininforService.deleteLogininforByIds(ids));
    }

    @DELETE
    @Path("/clean")
    @RequiresPermissions("monitor:logininfor:remove")
    public AjaxResult clean() {
        sysLogininforService.cleanLogininfor();
        return AjaxResult.success();
    }

    /**
     * 账户解锁：Quarkus 当前无 Spring 侧密码重试缓存，端点保留以兼容 SPA，返回成功。
     */
    @GET
    @Path("/unlock/{userName}")
    @RequiresPermissions("monitor:logininfor:unlock")
    public AjaxResult unlock(@PathParam("userName") String userName) {
        return AjaxResult.success();
    }

    private static SysLogininfor buildQuery(String ipaddr, String userName, String status,
                                            String beginTime, String endTime) {
        SysLogininfor query = new SysLogininfor();
        query.setIpaddr(ipaddr);
        query.setUserName(userName);
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

    private static SysLogininfor normalizeQuery(SysLogininfor query) {
        if (query == null) {
            query = new SysLogininfor();
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
