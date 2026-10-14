package com.cat2bug.framework.web;

import com.cat2bug.common.api.AjaxResult;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Open API 鉴权失败响应：使用真实 HTTP 401/403（与 Spring {@code ApiPermissionFilter} 对齐）。
 */
public final class ApiSecurityResponses {

    private ApiSecurityResponses() {
    }

    public static Response unauthorized(String message) {
        return Response.status(Response.Status.UNAUTHORIZED)
                .type(MediaType.APPLICATION_JSON)
                .entity(AjaxResult.error(AjaxResult.HTTP_UNAUTHORIZED, message))
                .build();
    }

    public static Response forbidden(String message) {
        return Response.status(Response.Status.FORBIDDEN)
                .type(MediaType.APPLICATION_JSON)
                .entity(AjaxResult.error(AjaxResult.HTTP_FORBIDDEN, message))
                .build();
    }
}
