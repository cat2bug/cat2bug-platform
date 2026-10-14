package com.cat2bug.admin.resource;

import com.cat2bug.common.api.AjaxResult;
import com.cat2bug.common.domain.LoginUser;
import com.cat2bug.common.domain.SysProjectMcp;
import com.cat2bug.common.domain.SysUser;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.common.security.McpModules;
import com.cat2bug.common.util.MessageUtils;
import com.cat2bug.framework.security.AuthContext;
import com.cat2bug.framework.security.RequiresPermissions;
import com.cat2bug.framework.security.token.McpPaths;
import com.cat2bug.framework.web.PublicUrlResolver;
import com.cat2bug.system.service.mcp.McpAccessService;
import com.cat2bug.system.service.mcp.McpProjectAccess;
import com.cat2bug.system.service.mcp.SysProjectMcpService;
import io.vertx.core.http.HttpServerRequest;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 项目 MCP 设置：是否允许经 MCP 访问本项目，以及开放哪些模块。读写都要求在目标项目内拥有对应角色权限。
 */
@Path("/system/project/{projectId:[0-9]+}/mcp")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProjectMcpResource {

    static final String QUERY_PERMISSION = "system:project:query";
    static final String EDIT_PERMISSION = "system:project:edit";

    public record McpSettingsBody(Boolean enabled, List<String> modules) {
    }

    @Inject
    AuthContext authContext;

    @Inject
    SysProjectMcpService sysProjectMcpService;

    @Inject
    McpAccessService mcpAccessService;

    @Inject
    PublicUrlResolver publicUrlResolver;

    @GET
    @RequiresPermissions(QUERY_PERMISSION)
    public AjaxResult get(@PathParam("projectId") Long projectId, @Context HttpServerRequest request) {
        McpProjectAccess access = requireAccess(projectId, QUERY_PERMISSION);
        return AjaxResult.success(view(sysProjectMcpService.get(projectId), access, request));
    }

    @PUT
    @RequiresPermissions(EDIT_PERMISSION)
    public AjaxResult save(@PathParam("projectId") Long projectId, McpSettingsBody body,
                           @Context HttpServerRequest request) {
        LoginUser loginUser = requireLoginUser();
        McpProjectAccess access = requireAccess(projectId, EDIT_PERMISSION);
        if (body == null) {
            throw new ApiException(400, "请求体不能为空");
        }
        List<String> modules = body.modules() == null ? List.of() : body.modules();
        for (String key : modules) {
            if (!McpModules.isKnown(key)) {
                throw new ApiException(400, MessageUtils.message("project.mcp.module-invalid", String.valueOf(key)));
            }
        }
        SysProjectMcp saved = sysProjectMcpService.save(projectId, Boolean.TRUE.equals(body.enabled()), modules,
                String.valueOf(loginUser.getUserId()));
        return AjaxResult.success(view(saved, access, request));
    }

    private Map<String, Object> view(SysProjectMcp settings, McpProjectAccess access, HttpServerRequest request) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("projectId", settings.getProjectId());
        out.put("enabled", settings.isEnabled());
        out.put("modules", settings.getModules() == null ? List.of() : settings.getModules());
        out.put("availableModules", availableModules());
        out.put("updateBy", settings.getUpdateBy());
        out.put("updateTime", settings.getUpdateTime());
        out.put("editable", access.hasRolePermission(EDIT_PERMISSION));
        out.put("mcpUrl", publicUrlResolver.resolve(request) + McpPaths.ROOT);
        out.put("publicUrlConfigured", publicUrlResolver.isConfigured());
        if (access.project() != null) {
            out.put("teamKey", access.project().getTeamKey());
            out.put("projectKey", access.project().getProjectKey());
        }
        return out;
    }

    private static List<Map<String, Object>> availableModules() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (McpModules.Module module : McpModules.ALL) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("key", module.key());
            m.put("permissionPrefixes", module.permissionPrefixes());
            list.add(m);
        }
        return list;
    }

    private McpProjectAccess requireAccess(Long projectId, String permission) {
        LoginUser loginUser = requireLoginUser();
        SysUser user = loginUser.getUser();
        if (user == null) {
            user = new SysUser();
            user.setUserId(loginUser.getUserId());
        }
        mcpAccessService.invalidateProject(projectId);
        McpProjectAccess access = mcpAccessService.access(user, projectId);
        if (access.project() == null) {
            throw new ApiException(404, MessageUtils.message("git.repo.project-not-found"));
        }
        if (!access.hasRolePermission(permission)) {
            throw new ApiException(403, MessageUtils.message("no-permission"));
        }
        return access;
    }

    private LoginUser requireLoginUser() {
        LoginUser loginUser = authContext.getLoginUser();
        if (loginUser == null) {
            throw new ApiException(401, "未登录或会话已失效");
        }
        return loginUser;
    }
}
