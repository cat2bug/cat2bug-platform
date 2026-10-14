package com.cat2bug.framework.security.open;

import com.cat2bug.common.domain.LoginUser;
import com.cat2bug.common.open.OpenApiKeyResolver;
import com.cat2bug.common.util.StringUtils;
import com.cat2bug.framework.security.AuthContext;
import com.cat2bug.framework.security.PublicPaths;
import com.cat2bug.framework.security.SecurityRequestContext;
import com.cat2bug.framework.web.ApiSecurityResponses;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;

@Provider
@Priority(900)
public class ApiKeyAuthFilter implements ContainerRequestFilter {

    @ConfigProperty(name = "cat2bug.api.enabled", defaultValue = "false")
    boolean apiEnabled;

    @Inject
    OpenApiKeyResolver openApiKeyResolver;

    @Inject
    AuthContext authContext;

    @Override
    public void filter(ContainerRequestContext ctx) throws IOException {
        String path = PublicPaths.normalize(ctx.getUriInfo().getPath());
        if (!OpenApiPaths.isOpenApi(path)) {
            return;
        }
        if (!apiEnabled) {
            ctx.abortWith(ApiSecurityResponses.forbidden("Open API 未启用"));
            return;
        }
        String apiKey = extractApiKey(ctx);
        if (StringUtils.isEmpty(apiKey)) {
            ctx.abortWith(ApiSecurityResponses.unauthorized("API KEY 不能为空"));
            return;
        }
        LoginUser loginUser = openApiKeyResolver.resolveLoginUser(apiKey);
        if (loginUser == null) {
            ctx.abortWith(ApiSecurityResponses.unauthorized("无效的 API KEY"));
            return;
        }
        SecurityRequestContext.setLoginUser(ctx, loginUser);
        authContext.setLoginUser(loginUser);
    }

    static String extractApiKey(ContainerRequestContext ctx) {
        String apiKey = ctx.getHeaderString(ApiKeyConstants.HEADER_NAME);
        if (StringUtils.isNotEmpty(apiKey)) {
            apiKey = apiKey.replace("Bearer ", "").trim();
        }
        return apiKey;
    }
}
