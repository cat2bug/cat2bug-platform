package com.cat2bug.quarkus.admin.resource;

import com.cat2bug.quarkus.admin.service.SystemExcelExportService;
import com.cat2bug.quarkus.common.api.AjaxResult;
import com.cat2bug.quarkus.common.api.TableDataInfo;
import com.cat2bug.quarkus.common.domain.LoginUser;
import com.cat2bug.quarkus.common.domain.SysDictType;
import com.cat2bug.quarkus.common.exception.ApiException;
import com.cat2bug.quarkus.framework.security.AuthContext;
import com.cat2bug.quarkus.framework.security.RequiresPermissions;
import com.cat2bug.quarkus.system.service.SysDictTypeService;
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

@Path("/system/dict/type")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DictTypeResource {

    static final String SPREADSHEET_MEDIA_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Inject
    AuthContext authContext;

    @Inject
    SysDictTypeService sysDictTypeService;

    @Inject
    SystemExcelExportService systemExcelExportService;

    @GET
    @Path("/list")
    @RequiresPermissions("system:dict:list")
    public TableDataInfo list(@QueryParam("dictName") String dictName,
                              @QueryParam("dictType") String dictType,
                              @QueryParam("status") String status,
                              @QueryParam("pageNum") Integer pageNum,
                              @QueryParam("pageSize") Integer pageSize) {
        SysDictType query = new SysDictType();
        query.setDictName(dictName);
        query.setDictType(dictType);
        query.setStatus(status);
        return sysDictTypeService.selectDictTypePage(query, pageNum, pageSize);
    }

    @POST
    @Path("/export")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(SPREADSHEET_MEDIA_TYPE)
    @RequiresPermissions("system:dict:export")
    public Response export(@RestForm("dictName") String dictName,
                           @RestForm("dictType") String dictType,
                           @RestForm("status") String status) {
        SysDictType query = new SysDictType();
        query.setDictName(dictName);
        query.setDictType(dictType);
        query.setStatus(status);
        byte[] workbook = systemExcelExportService.exportDictTypes(query);
        return Response.ok(workbook).type(SPREADSHEET_MEDIA_TYPE).build();
    }

    @GET
    @Path("/{dictId}")
    @RequiresPermissions("system:dict:query")
    public AjaxResult getInfo(@PathParam("dictId") Long dictId) {
        return AjaxResult.success(sysDictTypeService.selectDictTypeById(dictId));
    }

    @POST
    @RequiresPermissions("system:dict:add")
    public AjaxResult add(SysDictType dict) {
        if (!sysDictTypeService.checkDictTypeUnique(dict)) {
            return AjaxResult.error("新增字典'" + dict.getDictName() + "'失败，字典类型已存在");
        }
        LoginUser loginUser = requireLoginUser();
        dict.setCreateBy(loginUser.getUsername());
        return toAjax(sysDictTypeService.insertDictType(dict));
    }

    @PUT
    @RequiresPermissions("system:dict:edit")
    public AjaxResult edit(SysDictType dict) {
        if (!sysDictTypeService.checkDictTypeUnique(dict)) {
            return AjaxResult.error("修改字典'" + dict.getDictName() + "'失败，字典类型已存在");
        }
        LoginUser loginUser = requireLoginUser();
        dict.setUpdateBy(loginUser.getUsername());
        return toAjax(sysDictTypeService.updateDictType(dict));
    }

    @DELETE
    @Path("/{dictIds}")
    @RequiresPermissions("system:dict:remove")
    public AjaxResult remove(@PathParam("dictIds") String dictIds) {
        Long[] ids = Arrays.stream(dictIds.split(","))
                .map(String::trim)
                .map(Long::valueOf)
                .toArray(Long[]::new);
        sysDictTypeService.deleteDictTypeByIds(ids);
        return AjaxResult.success();
    }

    @DELETE
    @Path("/refreshCache")
    @RequiresPermissions("system:dict:remove")
    public AjaxResult refreshCache() {
        sysDictTypeService.resetDictCache();
        return AjaxResult.success();
    }

    @GET
    @Path("/optionselect")
    public AjaxResult optionselect() {
        return AjaxResult.success(sysDictTypeService.selectDictTypeAll());
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
