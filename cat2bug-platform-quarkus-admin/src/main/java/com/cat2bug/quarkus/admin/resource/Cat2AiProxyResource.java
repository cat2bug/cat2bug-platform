package com.cat2bug.quarkus.admin.resource;

import com.cat2bug.quarkus.admin.service.Cat2AiProxyService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

/**
 * Cat2AI 反向代理入口，对齐 Spring {@code Cat2AiProxyController}（{@code /cat2ai/**}）。
 */
@Path("/cat2ai")
public class Cat2AiProxyResource {

    @Inject
    Cat2AiProxyService cat2AiProxyService;

    @POST
    @Path("{path:.*}")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.WILDCARD)
    public Response proxy(@Context UriInfo uriInfo, String body) {
        return cat2AiProxyService.forward("POST", uriInfo, body);
    }
}
