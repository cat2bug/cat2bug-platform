package com.cat2bug.admin.resource;

import com.cat2bug.common.api.AjaxResult;
import com.cat2bug.common.domain.LoginUser;
import com.cat2bug.common.domain.SysUserWorkbench;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.framework.security.AuthContext;
import com.cat2bug.framework.security.RequiresPermissions;
import com.cat2bug.system.service.SysUserWorkbenchService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.HashMap;
import java.util.Map;

/**
 * 固定「工作台」个人布局（用户×项目）。自定义页签见 {@link WorkbenchTabResource}。
 */
@Path("/system/workbench")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class WorkbenchResource {

    @Inject
    AuthContext authContext;

    @Inject
    SysUserWorkbenchService sysUserWorkbenchService;

    @GET
    @Path("/{projectId}")
    @RequiresPermissions("system:dashboard:query")
    public AjaxResult getLayout(@PathParam("projectId") Long projectId) {
        LoginUser loginUser = requireLoginUser();
        SysUserWorkbench row = sysUserWorkbenchService.selectByUserAndProject(loginUser.getUserId(), projectId);
        Map<String, Object> body = new HashMap<>();
        body.put("projectId", projectId);
        body.put("userId", loginUser.getUserId());
        body.put("configJson", row == null ? null : row.getConfigJson());
        return AjaxResult.success(body);
    }

    @PUT
    @Path("/{projectId}")
    @RequiresPermissions("system:dashboard:query")
    public AjaxResult saveLayout(@PathParam("projectId") Long projectId, Map<String, Object> body) {
        LoginUser loginUser = requireLoginUser();
        Object config = body == null ? null : body.get("configJson");
        if (config == null && body != null && body.containsKey("config")) {
            config = body.get("config");
        }
        String configJson = config == null ? null : String.valueOf(config);
        if (configJson == null || configJson.isBlank() || "null".equals(configJson)) {
            throw new ApiException(400, "configJson 不能为空");
        }
        SysUserWorkbench saved = sysUserWorkbenchService.saveConfig(loginUser.getUserId(), projectId, configJson);
        Map<String, Object> resp = new HashMap<>();
        resp.put("projectId", projectId);
        resp.put("userId", loginUser.getUserId());
        resp.put("configJson", saved.getConfigJson());
        return AjaxResult.success(resp);
    }

    private LoginUser requireLoginUser() {
        LoginUser loginUser = authContext.getLoginUser();
        if (loginUser == null) {
            throw new ApiException(401, "未登录或会话已失效");
        }
        return loginUser;
    }
}
