package com.cat2bug.admin.resource;

import com.cat2bug.common.api.AjaxResult;
import com.cat2bug.common.util.MessageUtils;
import com.cat2bug.framework.security.SecurityUtils;
import com.cat2bug.framework.security.token.McpPaths;
import com.cat2bug.framework.web.PublicUrlResolver;
import com.cat2bug.system.service.SysUserAccessTokenService;
import io.vertx.core.http.HttpServerRequest;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 个人中心「访问令牌」：只管理当前登录用户自己的令牌，网页 JWT 登录态访问。
 */
@Path("/system/user/profile/tokens")
@Produces(MediaType.APPLICATION_JSON)
public class UserAccessTokenResource {

    @Inject
    SysUserAccessTokenService tokenService;

    @Inject
    PublicUrlResolver publicUrlResolver;

    public record CreateRequest(String name, Integer expireDays) {
    }

    @GET
    public AjaxResult list(@Context HttpServerRequest request) {
        AjaxResult result = AjaxResult.success(tokenService.listVisible(SecurityUtils.getUserId()));
        result.put("mcpUrl", publicUrlResolver.resolve(request) + McpPaths.ROOT);
        result.put("publicUrlConfigured", publicUrlResolver.isConfigured());
        return result;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public AjaxResult create(CreateRequest body) {
        SysUserAccessTokenService.CreatedToken created = tokenService.createPersonal(
                SecurityUtils.getUserId(), body == null ? null : body.name(), body == null ? null : body.expireDays());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("token", created.plaintext());
        data.put("info", created.token());
        return AjaxResult.success(data);
    }

    @DELETE
    @Path("/{tokenId:[0-9]+}")
    public AjaxResult revoke(@PathParam("tokenId") Long tokenId) {
        return tokenService.revoke(SecurityUtils.getUserId(), tokenId)
                ? AjaxResult.success()
                : AjaxResult.error(MessageUtils.message("user.access-token.not-found"));
    }
}
