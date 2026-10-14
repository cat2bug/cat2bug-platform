package com.cat2bug.framework.security;

import com.cat2bug.common.domain.LoginUser;
import com.cat2bug.framework.security.git.GitSmartHttpPathSupport;
import com.cat2bug.framework.security.open.OpenApiPaths;
import com.cat2bug.framework.web.SecurityResponses;
import jakarta.ws.rs.Priorities;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class JwtAuthFilter implements ContainerRequestFilter {

    @Inject
    LoginUserResolver loginUserResolver;

    @Override
    public void filter(ContainerRequestContext ctx) throws IOException {
        String path = PublicPaths.normalize(ctx.getUriInfo().getPath());
        if (OpenApiPaths.isOpenApi(path)) {
            return;
        }
        if (PublicPaths.isPublic(path)) {
            LoginUser optionalUser = loginUserResolver.resolve(ctx);
            if (optionalUser != null) {
                SecurityRequestContext.setLoginUser(ctx, optionalUser);
            }
            return;
        }
        // Smart HTTP 使用 Basic 鉴权，由 GitSmartHttpResource 自行校验
        String absolutePath = "/" + path;
        if (GitSmartHttpPathSupport.matchesGitSecurityChain(absolutePath)) {
            return;
        }
        LoginUser loginUser = loginUserResolver.resolve(ctx);
        if (loginUser == null) {
            ctx.abortWith(SecurityResponses.unauthorized(ctx.getUriInfo().getPath()));
            return;
        }
        SecurityRequestContext.setLoginUser(ctx, loginUser);
    }
}
