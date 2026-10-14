package com.cat2bug.admin.resource;

import com.cat2bug.admin.service.SystemExcelExportService;
import com.cat2bug.common.api.AjaxResult;
import com.cat2bug.common.api.TableDataInfo;
import com.cat2bug.common.domain.LoginUser;
import com.cat2bug.common.domain.SysConfig;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.framework.security.AuthContext;
import com.cat2bug.framework.security.RequiresPermissions;
import com.cat2bug.system.service.SysConfigService;
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

@Path("/system/config")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ConfigResource {

    static final String SPREADSHEET_MEDIA_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Inject
    AuthContext authContext;

    @Inject
    SysConfigService sysConfigService;

    @Inject
    SystemExcelExportService systemExcelExportService;

    @GET
    @Path("/list")
    @RequiresPermissions("system:config:list")
    public TableDataInfo list(@QueryParam("configName") String configName,
                              @QueryParam("configKey") String configKey,
                              @QueryParam("configType") String configType,
                              @QueryParam("pageNum") Integer pageNum,
                              @QueryParam("pageSize") Integer pageSize) {
        SysConfig query = new SysConfig();
        query.setConfigName(configName);
        query.setConfigKey(configKey);
        query.setConfigType(configType);
        return sysConfigService.selectConfigPage(query, pageNum, pageSize);
    }

    @POST
    @Path("/export")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(SPREADSHEET_MEDIA_TYPE)
    @RequiresPermissions("system:config:export")
    public Response export(@RestForm("configName") String configName,
                           @RestForm("configKey") String configKey,
                           @RestForm("configType") String configType) {
        SysConfig query = new SysConfig();
        query.setConfigName(configName);
        query.setConfigKey(configKey);
        query.setConfigType(configType);
        byte[] workbook = systemExcelExportService.exportConfigs(query);
        return Response.ok(workbook).type(SPREADSHEET_MEDIA_TYPE).build();
    }

    @GET
    @Path("/{configId}")
    @RequiresPermissions("system:config:query")
    public AjaxResult getInfo(@PathParam("configId") Long configId) {
        return AjaxResult.success(sysConfigService.selectConfigById(configId));
    }

    @GET
    @Path("/configKey/{configKey}")
    public AjaxResult getConfigKey(@PathParam("configKey") String configKey) {
        return AjaxResult.success(sysConfigService.selectConfigByKey(configKey));
    }

    @POST
    @RequiresPermissions("system:config:add")
    public AjaxResult add(SysConfig config) {
        if (!sysConfigService.checkConfigKeyUnique(config)) {
            return AjaxResult.error("新增参数'" + config.getConfigName() + "'失败，参数键名已存在");
        }
        LoginUser loginUser = requireLoginUser();
        config.setCreateBy(loginUser.getUsername());
        return toAjax(sysConfigService.insertConfig(config));
    }

    @PUT
    @RequiresPermissions("system:config:edit")
    public AjaxResult edit(SysConfig config) {
        if (!sysConfigService.checkConfigKeyUnique(config)) {
            return AjaxResult.error("修改参数'" + config.getConfigName() + "'失败，参数键名已存在");
        }
        requireLoginUser();
        return toAjax(sysConfigService.updateConfig(config));
    }

    @DELETE
    @Path("/{configIds}")
    @RequiresPermissions("system:config:remove")
    public AjaxResult remove(@PathParam("configIds") String configIds) {
        Long[] ids = Arrays.stream(configIds.split(","))
                .map(String::trim)
                .map(Long::valueOf)
                .toArray(Long[]::new);
        sysConfigService.deleteConfigByIds(ids);
        return AjaxResult.success();
    }

    @DELETE
    @Path("/refreshCache")
    @RequiresPermissions("system:config:remove")
    public AjaxResult refreshCache() {
        sysConfigService.resetConfigCache();
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
