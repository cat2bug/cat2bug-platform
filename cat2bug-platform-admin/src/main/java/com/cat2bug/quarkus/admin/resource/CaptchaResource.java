package com.cat2bug.quarkus.admin.resource;

import com.cat2bug.quarkus.admin.service.CaptchaService;
import com.cat2bug.quarkus.common.api.AjaxResult;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/captchaImage")
@Produces(MediaType.APPLICATION_JSON)
public class CaptchaResource {

    @Inject
    CaptchaService captchaService;

    @GET
    public AjaxResult getCode() {
        return captchaService.createCaptchaImage();
    }
}
