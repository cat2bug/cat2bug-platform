package com.cat2bug.admin.resource;

import com.cat2bug.admin.service.RegisterService;
import com.cat2bug.common.api.AjaxResult;
import com.cat2bug.common.api.RegisterBody;
import com.cat2bug.system.service.SysConfigService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/register")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RegisterResource {

    @Inject
    RegisterService registerService;

    @Inject
    SysConfigService sysConfigService;

    @POST
    public AjaxResult register(RegisterBody body) {
        if (!sysConfigService.selectRegisterEnabled()) {
            return AjaxResult.error("当前系统没有开启注册功能！");
        }
        try {
            registerService.register(body);
            return AjaxResult.success();
        } catch (RuntimeException e) {
            return AjaxResult.error(e.getMessage());
        }
    }
}
