package com.cat2bug.quarkus.framework.web;

import com.cat2bug.quarkus.common.api.AjaxResult;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

/**
 * 拒绝非法排序参数（Spring 在 {@code startPage()} 中抛 UtilException，返回 code=500 的 AjaxResult）。
 */
@Provider
public class PageOrderFilter implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext requestContext) {
        MultivaluedMap<String, String> query = requestContext.getUriInfo().getQueryParameters();
        String orderBy = PageOrder.build(query.getFirst(PageOrder.PARAM_COLUMN), query.getFirst(PageOrder.PARAM_ASC));
        String error = PageOrder.validationError(orderBy);
        if (error != null) {
            requestContext.abortWith(Response.ok(AjaxResult.error(error), MediaType.APPLICATION_JSON_TYPE).build());
        }
    }
}
