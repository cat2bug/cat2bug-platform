package com.cat2bug.quarkus.framework.security;

import com.cat2bug.quarkus.common.domain.LoginUser;
import com.cat2bug.quarkus.framework.security.open.OpenApiPaths;
import com.cat2bug.quarkus.framework.web.SecurityResponses;
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
            return;
        }
        LoginUser loginUser = loginUserResolver.resolve(ctx);
        if (loginUser == null) {
            ctx.abortWith(SecurityResponses.unauthorized(ctx.getUriInfo().getPath()));
        }
    }
}
