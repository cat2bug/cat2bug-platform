package com.cat2bug.admin.security;

import com.cat2bug.common.api.AjaxResult;
import com.cat2bug.common.domain.LoginUser;
import com.cat2bug.common.domain.ModulePageSettings;
import com.cat2bug.common.domain.SysProjectApi;
import com.cat2bug.common.domain.SysUserConfig;
import com.cat2bug.common.open.OpenApiKeyResolver;
import com.cat2bug.common.security.TeamModuleGates;
import com.cat2bug.common.util.MessageUtils;
import com.cat2bug.common.util.TeamModuleLabelContext;
import com.cat2bug.framework.security.LoginUserResolver;
import com.cat2bug.framework.security.RequiresPermissions;
import com.cat2bug.framework.security.git.GitSmartHttpPathSupport;
import com.cat2bug.framework.security.open.ApiKeyConstants;
import com.cat2bug.framework.security.open.OpenApiPaths;
import com.cat2bug.framework.web.ApiSecurityResponses;
import com.cat2bug.system.mapper.SysProjectMapper;
import com.cat2bug.system.service.SysModulePageSettingsService;
import com.cat2bug.system.service.SysUserConfigService;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Optional;

/**
 * 团队功能模块硬闸门：关闭则业务 API（含 Open API 与 Git）返回 403。
 * 团队设置与项目预配置入口不拦截。能解析到团队时写入显示名上下文，供文案与导出表头覆盖。
 */
@Provider
@Priority(Priorities.AUTHORIZATION + 100)
public class TeamModuleGateFilter implements ContainerRequestFilter, ContainerResponseFilter {

    @Context
    ResourceInfo resourceInfo;

    @Inject
    SysModulePageSettingsService modulePageSettingsService;

    @Inject
    SysUserConfigService userConfigService;

    @Inject
    LoginUserResolver loginUserResolver;

    @Inject
    SysProjectMapper sysProjectMapper;

    @Inject
    OpenApiKeyResolver openApiKeyResolver;

    @Override
    public void filter(ContainerRequestContext ctx) throws IOException {
        TeamModuleLabelContext.clear();
        String path = strip(ctx.getUriInfo().getPath());
        Long projectId = resolveProjectId(ctx, path);
        Long teamId = resolveTeamId(path, projectId);
        ModulePageSettings settings = loadSettings(teamId);
        if (settings != null) {
            TeamModuleLabelContext.set(settings);
        }
        if (TeamModuleGates.exempt(path) || settings == null) {
            return;
        }
        String gate = resolveGate(path, settings);
        if (gate == null || settings.isEnabled(gate)) {
            return;
        }
        String message = MessageUtils.message("module.page-settings.disabled");
        if (useHttpStatus(path)) {
            ctx.abortWith(ApiSecurityResponses.forbidden(message));
        } else {
            ctx.abortWith(Response.ok(AjaxResult.error(AjaxResult.HTTP_FORBIDDEN, message))
                    .type(MediaType.APPLICATION_JSON)
                    .build());
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        TeamModuleLabelContext.clear();
    }

    private String resolveGate(String path, ModulePageSettings settings) {
        RequiresPermissions annotation = resolveAnnotation();
        if (annotation != null) {
            boolean or = annotation.logical() == RequiresPermissions.Logical.OR;
            return TeamModuleGates.closedGate(or, annotation.value(), settings::isEnabled);
        }
        String gate = TeamModuleGates.gateOfPath(path);
        if (gate != null && !settings.isEnabled(gate)) {
            return gate;
        }
        return null;
    }

    private ModulePageSettings loadSettings(Long teamId) {
        if (teamId == null) {
            return null;
        }
        try {
            return modulePageSettingsService.getByTeamId(teamId);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private Long resolveTeamId(String path, Long projectId) {
        Long pathTeamId = firstLong(path, "system/team/");
        if (pathTeamId != null && path.startsWith("system/team/")) {
            return pathTeamId;
        }
        if (projectId == null) {
            return null;
        }
        try {
            return modulePageSettingsService.teamIdOfProject(projectId);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private Long resolveProjectId(ContainerRequestContext ctx, String path) {
        Long fromPath = firstLong(path, "system/project/");
        if (fromPath != null) {
            return fromPath;
        }
        if (OpenApiPaths.isOpenApi(path)) {
            Long apiProjectId = openApiProjectId(ctx);
            if (apiProjectId != null) {
                return apiProjectId;
            }
        }
        Long queryId = parseLong(ctx.getUriInfo().getQueryParameters().getFirst("projectId"));
        if (queryId != null) {
            return queryId;
        }
        LoginUser user = loginUserResolver.resolve(ctx);
        if (user != null && user.getMcpProjectId() != null && user.getMcpProjectId() > 0) {
            return user.getMcpProjectId();
        }
        Optional<GitSmartHttpPathSupport.GitSmartHttpPathMatch> git =
                GitSmartHttpPathSupport.parsePackPath("/" + path);
        if (git.isPresent()) {
            GitSmartHttpPathSupport.GitSmartHttpPathMatch match = git.get();
            if (match.getProjectId() != null) {
                return match.getProjectId();
            }
            if (match.getTeamKey() != null && match.getProjectKey() != null) {
                Long id = lookupProject(match.getTeamKey(), match.getProjectKey());
                if (id != null) {
                    return id;
                }
            }
        }
        if (user != null && user.getUserId() != null) {
            SysUserConfig config = userConfigService.selectSysUserConfigByUserId(user.getUserId());
            if (config != null && config.getCurrentProjectId() != null && config.getCurrentProjectId() > 0) {
                return config.getCurrentProjectId();
            }
        }
        return null;
    }

    private Long openApiProjectId(ContainerRequestContext ctx) {
        String apiKey = ctx.getHeaderString(ApiKeyConstants.HEADER_NAME);
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }
        apiKey = apiKey.replace("Bearer ", "").trim();
        try {
            SysProjectApi projectApi = openApiKeyResolver.findProjectApi(apiKey);
            return projectApi == null ? null : projectApi.getProjectId();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private Long lookupProject(String teamKey, String projectKey) {
        try {
            var project = sysProjectMapper.selectSysProjectByTeamAndProjectKey(teamKey, projectKey);
            return project == null ? null : project.getProjectId();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private RequiresPermissions resolveAnnotation() {
        if (resourceInfo == null) {
            return null;
        }
        Method method = resourceInfo.getResourceMethod();
        if (method != null && method.isAnnotationPresent(RequiresPermissions.class)) {
            return method.getAnnotation(RequiresPermissions.class);
        }
        Class<?> clazz = resourceInfo.getResourceClass();
        if (clazz != null && clazz.isAnnotationPresent(RequiresPermissions.class)) {
            return clazz.getAnnotation(RequiresPermissions.class);
        }
        return null;
    }

    private static boolean useHttpStatus(String path) {
        return OpenApiPaths.isOpenApi(path)
                || path.startsWith("git/")
                || path.matches("[^/]+/[^/]+/code/[^/]+\\.git(/.*)?");
    }

    private static Long firstLong(String path, String prefix) {
        if (path == null || !path.startsWith(prefix)) {
            return null;
        }
        String rest = path.substring(prefix.length());
        int slash = rest.indexOf('/');
        String token = slash < 0 ? rest : rest.substring(0, slash);
        return parseLong(token);
    }

    private static Long parseLong(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            long value = Long.parseLong(raw.trim());
            return value > 0 ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String strip(String path) {
        if (path == null) {
            return "";
        }
        return path.startsWith("/") ? path.substring(1) : path;
    }
}
