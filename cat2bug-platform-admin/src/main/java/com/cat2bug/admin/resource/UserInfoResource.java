package com.cat2bug.admin.resource;

import com.cat2bug.common.api.AjaxResult;
import com.cat2bug.common.domain.LoginUser;
import com.cat2bug.common.domain.SysUser;
import com.cat2bug.common.domain.SysUserConfig;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.framework.security.AuthContext;
import com.cat2bug.system.service.SysMenuService;
import com.cat2bug.system.service.SysPermissionService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.HashMap;
import java.util.Map;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class UserInfoResource {

    @Inject
    AuthContext authContext;

    @Inject
    SysPermissionService permissionService;

    @Inject
    SysMenuService menuService;

    @GET
    @Path("getInfo")
    public AjaxResult getInfo() {
        LoginUser loginUser = requireLoginUser();
        SysUser user = loginUser.getUser();

        Map<String, Object> safeUser = new HashMap<>();
        safeUser.put("userId", user.getUserId());
        safeUser.put("userName", user.getUserName());
        safeUser.put("nickName", user.getNickName() != null ? user.getNickName() : user.getUserName());
        safeUser.put("avatar", user.getAvatar());
        safeUser.put("phoneNumber", user.getPhoneNumber());

        SysUserConfig config = permissionService.getUserConfig(user.getUserId());

        AjaxResult ajax = AjaxResult.success();
        ajax.put("user", safeUser);
        ajax.put("roles", permissionService.getRoleKeys(user));
        ajax.put("permissions", loginUser.getPermissions());
        ajax.put("config", config != null ? config : Map.of());
        return ajax;
    }

    @GET
    @Path("getRouters")
    public AjaxResult getRouters() {
        LoginUser loginUser = requireLoginUser();
        return AjaxResult.success(menuService.buildMenus(
                menuService.selectMenuTreeByUserId(loginUser.getUserId())));
    }

    private LoginUser requireLoginUser() {
        LoginUser loginUser = authContext.getLoginUser();
        if (loginUser == null) {
            throw new ApiException(401, "未登录或会话已失效");
        }
        return loginUser;
    }
}
