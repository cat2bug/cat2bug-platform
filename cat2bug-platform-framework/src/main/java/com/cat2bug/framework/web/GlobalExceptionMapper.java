package com.cat2bug.framework.web;

import com.cat2bug.common.api.AjaxResult;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.common.exception.CaseImportValidationException;
import com.cat2bug.common.exception.WechatMpNotBindException;
import com.cat2bug.framework.web.SecurityResponses;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Exception> {

    private static final Logger LOG = Logger.getLogger(GlobalExceptionMapper.class);

    @Override
    public Response toResponse(Exception exception) {
        if (exception instanceof CaseImportValidationException validationException) {
            return Response.ok(AjaxResult.success(validationException.getResult())).build();
        }
        if (exception instanceof WechatMpNotBindException notBindException) {
            AjaxResult body = AjaxResult.error(notBindException.getHttpStatus(), notBindException.getMessage());
            body.put("sessionKey", notBindException.getSessionKey());
            return Response.ok(body).build();
        }
        if (exception instanceof ApiException apiException) {
            int status = apiException.getHttpStatus();
            AjaxResult body = AjaxResult.error(status, apiException.getMessage());
            if (status == AjaxResult.HTTP_UNAUTHORIZED) {
                return Response.ok(AjaxResult.error(status, apiException.getMessage())).build();
            }
            if (status == AjaxResult.HTTP_FORBIDDEN) {
                return SecurityResponses.forbidden();
            }
            return Response.status(status).entity(body).build();
        }
        if (exception instanceof NotFoundException) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(AjaxResult.error("接口不存在"))
                    .build();
        }
        if (exception instanceof WebApplicationException webException) {
            Response response = webException.getResponse();
            if (response != null) {
                return response;
            }
        }
        LOG.error("Unhandled exception", exception);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(AjaxResult.error("服务器内部错误"))
                .build();
    }
}
