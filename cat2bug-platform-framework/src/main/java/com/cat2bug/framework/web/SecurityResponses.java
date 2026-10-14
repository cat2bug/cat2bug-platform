package com.cat2bug.framework.web;

import com.cat2bug.common.api.AjaxResult;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * 与 Spring {@code AuthenticationEntryPointImpl} / {@code GlobalExceptionHandler} 对齐：
 * 认证/授权失败返回 HTTP 200 + JSON {@code code} 401/403，供前端 axios 统一处理。
 */
public final class SecurityResponses {

    private SecurityResponses() {
    }

    public static Response unauthorized(String requestPath) {
        String path = requestPath == null ? "" : requestPath;
        String msg = "请求访问：" + path + "，认证失败，无法访问系统资源";
        return Response.ok(AjaxResult.error(AjaxResult.HTTP_UNAUTHORIZED, msg))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    public static Response forbidden() {
        return Response.ok(AjaxResult.error(AjaxResult.HTTP_FORBIDDEN, "没有权限，请联系管理员授权"))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
