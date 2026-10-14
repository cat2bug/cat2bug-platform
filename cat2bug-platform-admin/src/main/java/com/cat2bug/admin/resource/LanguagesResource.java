package com.cat2bug.admin.resource;

import com.cat2bug.common.api.AjaxResult;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * 语言切换（Spring {@code SysLanguagesController} 等价桩）。
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class LanguagesResource {

    @POST
    @Path("{lang}")
    public AjaxResult changeLanguage(@PathParam("lang") String lang) {
        return AjaxResult.success();
    }
}
