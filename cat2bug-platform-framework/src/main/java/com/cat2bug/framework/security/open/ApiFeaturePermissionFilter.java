package com.cat2bug.framework.security.open;

import com.cat2bug.common.domain.SysProjectApi;
import com.cat2bug.common.open.OpenApiKeyResolver;
import com.cat2bug.common.util.StringUtils;
import com.cat2bug.framework.security.PublicPaths;
import com.cat2bug.framework.web.ApiSecurityResponses;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;

@Provider
@Priority(1500)
public class ApiFeaturePermissionFilter implements ContainerRequestFilter {

    @ConfigProperty(name = "cat2bug.api.enabled", defaultValue = "false")
    boolean apiEnabled;

    @Inject
    OpenApiKeyResolver openApiKeyResolver;

    @Inject
    ObjectMapper objectMapper;

    @Override
    public void filter(ContainerRequestContext ctx) throws IOException {
        String path = PublicPaths.normalize(ctx.getUriInfo().getPath());
        if (!apiEnabled || !OpenApiPaths.isOpenApi(path)) {
            return;
        }
        String apiKey = ApiKeyAuthFilter.extractApiKey(ctx);
        if (StringUtils.isEmpty(apiKey)) {
            return;
        }
        SysProjectApi projectApi = openApiKeyResolver.findProjectApi(apiKey);
        if (projectApi == null) {
            return;
        }
        if (projectApi.getEnabled() != null && !projectApi.getEnabled()) {
            ctx.abortWith(ApiSecurityResponses.unauthorized("API KEY 已禁用"));
            return;
        }
        String method = ctx.getMethod();
        String required = ApiPermissionRegistry.requiredPermission(method, "/" + path);
        if (!ApiPermissionRegistry.hasFeaturePermission(projectApi.getFeatures(), required, objectMapper)) {
            ctx.abortWith(ApiSecurityResponses.forbidden("无权限访问此资源"));
        }
    }
}
