package com.cat2bug.quarkus.framework.security;

import com.cat2bug.quarkus.common.domain.LoginUser;
import com.cat2bug.quarkus.framework.web.SecurityResponses;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.lang.reflect.Method;

@Provider
@Priority(Priorities.AUTHORIZATION)
public class PermissionAuthorizationFilter implements ContainerRequestFilter {

    @Context
    ResourceInfo resourceInfo;

    @Inject
    LoginUserResolver loginUserResolver;

    @Inject
    PermissionChecker permissionChecker;

    @Override
    public void filter(ContainerRequestContext ctx) throws IOException {
        RequiresPermissions annotation = resolveAnnotation();
        if (annotation == null) {
            return;
        }
        LoginUser loginUser = loginUserResolver.resolve(ctx);
        if (loginUser == null) {
            ctx.abortWith(SecurityResponses.unauthorized(ctx.getUriInfo().getPath()));
            return;
        }
        if (!checkPermissions(loginUser, annotation)) {
            ctx.abortWith(SecurityResponses.forbidden());
        }
    }

    private RequiresPermissions resolveAnnotation() {
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

    private boolean checkPermissions(LoginUser loginUser, RequiresPermissions annotation) {
        String[] perms = annotation.value();
        if (perms == null || perms.length == 0) {
            return true;
        }
        if (annotation.logical() == RequiresPermissions.Logical.OR) {
            return permissionChecker.hasAnyPermi(loginUser, perms);
        }
        for (String perm : perms) {
            if (!permissionChecker.hasPermi(loginUser, perm)) {
                return false;
            }
        }
        return true;
    }
}
