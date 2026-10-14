package com.cat2bug.admin.resource;

import com.cat2bug.admin.service.SystemExcelExportService;
import com.cat2bug.common.api.AjaxResult;
import com.cat2bug.common.api.TableDataInfo;
import com.cat2bug.common.domain.LoginUser;
import com.cat2bug.common.domain.SysDictData;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.framework.security.AuthContext;
import com.cat2bug.framework.security.RequiresPermissions;
import com.cat2bug.system.service.SysDictDataService;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Path("/system/dict/data")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DictDataResource {

    static final String SPREADSHEET_MEDIA_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Inject
    AuthContext authContext;

    @Inject
    SysDictDataService sysDictDataService;

    @Inject
    SystemExcelExportService systemExcelExportService;

    @GET
    @Path("/list")
    @RequiresPermissions("system:dict:list")
    public TableDataInfo list(@QueryParam("dictType") String dictType,
                              @QueryParam("dictLabel") String dictLabel,
                              @QueryParam("status") String status,
                              @QueryParam("pageNum") Integer pageNum,
                              @QueryParam("pageSize") Integer pageSize) {
        SysDictData query = new SysDictData();
        query.setDictType(dictType);
        query.setDictLabel(dictLabel);
        query.setStatus(status);
        return sysDictDataService.selectDictDataPage(query, pageNum, pageSize);
    }

    @POST
    @Path("/export")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(SPREADSHEET_MEDIA_TYPE)
    @RequiresPermissions("system:dict:export")
    public Response export(@RestForm("dictType") String dictType,
                           @RestForm("dictLabel") String dictLabel,
                           @RestForm("status") String status) {
        SysDictData query = new SysDictData();
        query.setDictType(dictType);
        query.setDictLabel(dictLabel);
        query.setStatus(status);
        byte[] workbook = systemExcelExportService.exportDictData(query);
        return Response.ok(workbook).type(SPREADSHEET_MEDIA_TYPE).build();
    }

    @GET
    @Path("/type/{dictType}")
    public AjaxResult dictType(@PathParam("dictType") String dictType) {
        List<SysDictData> data = sysDictDataService.selectDictDataByType(dictType);
        if (data == null) {
            data = new ArrayList<>();
        }
        return AjaxResult.success(data);
    }

    @GET
    @Path("/{dictCode}")
    @RequiresPermissions("system:dict:query")
    public AjaxResult getInfo(@PathParam("dictCode") Long dictCode) {
        return AjaxResult.success(sysDictDataService.selectDictDataById(dictCode));
    }

    @POST
    @RequiresPermissions("system:dict:add")
    public AjaxResult add(SysDictData dict) {
        LoginUser loginUser = requireLoginUser();
        dict.setCreateBy(loginUser.getUsername());
        return toAjax(sysDictDataService.insertDictData(dict));
    }

    @PUT
    @RequiresPermissions("system:dict:edit")
    public AjaxResult edit(SysDictData dict) {
        requireLoginUser();
        return toAjax(sysDictDataService.updateDictData(dict));
    }

    @DELETE
    @Path("/{dictCodes}")
    @RequiresPermissions("system:dict:remove")
    public AjaxResult remove(@PathParam("dictCodes") String dictCodes) {
        Long[] ids = Arrays.stream(dictCodes.split(","))
                .map(String::trim)
                .map(Long::valueOf)
                .toArray(Long[]::new);
        sysDictDataService.deleteDictDataByIds(ids);
        return AjaxResult.success();
    }

    private LoginUser requireLoginUser() {
        LoginUser loginUser = authContext.getLoginUser();
        if (loginUser == null) {
            throw new ApiException(401, "未登录或会话已失效");
        }
        return loginUser;
    }

    private AjaxResult toAjax(int rows) {
        return rows > 0 ? AjaxResult.success() : AjaxResult.error("操作失败");
    }
}
