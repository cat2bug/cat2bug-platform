package com.cat2bug.admin.resource;

import com.cat2bug.common.api.AjaxResult;
import com.cat2bug.common.domain.LoginUser;
import com.cat2bug.common.domain.SysProjectWorkbenchTab;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.framework.security.AuthContext;
import com.cat2bug.framework.security.RequiresPermissions;
import com.cat2bug.system.service.SysProjectWorkbenchTabService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作台自定义页签。固定「工作台」个人布局仍走 {@link WorkbenchResource}。
 */
@Path("/system/workbench/{projectId}/tabs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class WorkbenchTabResource {

    @Inject
    AuthContext authContext;

    @Inject
    SysProjectWorkbenchTabService tabService;

    @GET
    @RequiresPermissions("system:dashboard:query")
    public AjaxResult list(@PathParam("projectId") Long projectId) {
        LoginUser loginUser = requireLoginUser();
        Long userId = loginUser.getUserId();
        List<SysProjectWorkbenchTab> tabs = tabService.listVisible(projectId, userId);
        Map<String, Object> body = new HashMap<>();
        body.put("tabs", tabs);
        body.put("sharedCount", tabService.countShared(projectId));
        body.put("sharedLimit", SysProjectWorkbenchTab.MAX_SHARED_PER_PROJECT);
        body.put("canManageShared", tabService.canManageShared(projectId, userId));
        body.put("projectRoleKeys", tabService.listProjectRoleKeys(projectId, userId));
        return AjaxResult.success(body);
    }

    @GET
    @Path("/{tabId}")
    @RequiresPermissions("system:dashboard:query")
    public AjaxResult get(@PathParam("projectId") Long projectId, @PathParam("tabId") Long tabId) {
        LoginUser loginUser = requireLoginUser();
        SysProjectWorkbenchTab tab = tabService.getVisibleOrThrow(tabId, loginUser.getUserId());
        assertSameProject(projectId, tab);
        return AjaxResult.success(tab);
    }

    @POST
    @RequiresPermissions("system:dashboard:query")
    public AjaxResult create(@PathParam("projectId") Long projectId, Map<String, Object> body) {
        LoginUser loginUser = requireLoginUser();
        String tabName = body == null ? null : stringOf(body.get("tabName"));
        String preset = body == null ? null : stringOf(body.get("preset"));
        String visibility = body == null ? null : stringOf(body.get("visibility"));
        String configJson = extractConfigJson(body);
        SysProjectWorkbenchTab created = tabService.create(
                projectId, loginUser.getUserId(), tabName, preset, visibility, configJson);
        return AjaxResult.success(created);
    }

    @PUT
    @Path("/sort")
    @RequiresPermissions("system:dashboard:query")
    public AjaxResult sort(@PathParam("projectId") Long projectId, List<Map<String, Object>> body) {
        LoginUser loginUser = requireLoginUser();
        List<SysProjectWorkbenchTab> items = new ArrayList<>();
        if (body != null) {
            for (Map<String, Object> row : body) {
                if (row == null) {
                    continue;
                }
                SysProjectWorkbenchTab item = new SysProjectWorkbenchTab();
                Object id = row.get("tabId");
                Object sort = row.get("tabSort");
                if (id != null) {
                    item.setTabId(Long.valueOf(String.valueOf(id)));
                }
                if (sort != null) {
                    item.setTabSort(Long.valueOf(String.valueOf(sort)));
                }
                items.add(item);
            }
        }
        tabService.reorder(projectId, loginUser.getUserId(), items);
        return AjaxResult.success();
    }

    @PUT
    @Path("/{tabId}")
    @RequiresPermissions("system:dashboard:query")
    public AjaxResult update(@PathParam("projectId") Long projectId,
                             @PathParam("tabId") Long tabId,
                             Map<String, Object> body) {
        LoginUser loginUser = requireLoginUser();
        SysProjectWorkbenchTab existing = tabService.getVisibleOrThrow(tabId, loginUser.getUserId());
        assertSameProject(projectId, existing);
        String tabName = body != null && body.containsKey("tabName") ? stringOf(body.get("tabName")) : null;
        String preset = body != null && body.containsKey("preset") ? stringOf(body.get("preset")) : null;
        String configJson = null;
        if (body != null && (body.containsKey("configJson") || body.containsKey("config"))) {
            configJson = extractConfigJson(body);
        }
        SysProjectWorkbenchTab updated = tabService.update(
                tabId, loginUser.getUserId(), tabName, preset, configJson);
        return AjaxResult.success(updated);
    }

    @DELETE
    @Path("/{tabId}")
    @RequiresPermissions("system:dashboard:query")
    public AjaxResult delete(@PathParam("projectId") Long projectId, @PathParam("tabId") Long tabId) {
        LoginUser loginUser = requireLoginUser();
        SysProjectWorkbenchTab existing = tabService.getVisibleOrThrow(tabId, loginUser.getUserId());
        assertSameProject(projectId, existing);
        tabService.delete(tabId, loginUser.getUserId());
        return AjaxResult.success();
    }

    private static void assertSameProject(Long projectId, SysProjectWorkbenchTab tab) {
        if (tab.getProjectId() == null || !tab.getProjectId().equals(projectId)) {
            throw new ApiException(404, "页签不存在");
        }
    }

    private static String extractConfigJson(Map<String, Object> body) {
        if (body == null) {
            return null;
        }
        Object config = body.get("configJson");
        if (config == null && body.containsKey("config")) {
            config = body.get("config");
        }
        if (config == null) {
            return null;
        }
        if (config instanceof String) {
            return (String) config;
        }
        // Map / List：交给前端已 stringify；若直接传对象则 toString 不可靠，要求字符串
        throw new ApiException(400, "configJson 须为 JSON 字符串");
    }

    private static String stringOf(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    private LoginUser requireLoginUser() {
        LoginUser loginUser = authContext.getLoginUser();
        if (loginUser == null) {
            throw new ApiException(401, "未登录或会话已失效");
        }
        return loginUser;
    }
}
