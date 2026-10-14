package com.cat2bug.admin.resource;

import com.cat2bug.common.api.AjaxResult;
import com.cat2bug.common.domain.LoginUser;
import com.cat2bug.common.domain.SysUserConfig;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.framework.security.AuthContext;
import com.cat2bug.framework.security.SecurityUtils;
import com.cat2bug.system.service.SysPermissionService;
import com.cat2bug.system.service.SysUserConfigService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.Arrays;

@Path("/system/user-config")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserConfigResource {

    @Inject
    AuthContext authContext;

    @Inject
    SysUserConfigService sysUserConfigService;

    @Inject
    SysPermissionService permissionService;

    @GET
    @Path("/{userConfigId}")
    public AjaxResult getInfo(@PathParam("userConfigId") Long userConfigId) {
        return AjaxResult.success(sysUserConfigService.selectSysUserConfigByUserConfigId(userConfigId));
    }

    @POST
    public AjaxResult add(SysUserConfig sysUserConfig) {
        LoginUser loginUser = requireLoginUser();
        if (!SecurityUtils.isAdmin() || sysUserConfig.getUserId() == null) {
            sysUserConfig.setUserId(loginUser.getUserId());
        }
        int rows = sysUserConfigService.insertSysUserConfig(sysUserConfig);
        permissionService.updateRoleAndPermissionOfCurrentUser(loginUser);
        return rows > 0 ? AjaxResult.success(sysUserConfig) : AjaxResult.error("新增用户配置失败");
    }

    @DELETE
    @Path("/{userConfigIds}")
    public AjaxResult remove(@PathParam("userConfigIds") String userConfigIds) {
        LoginUser loginUser = requireLoginUser();
        Long[] ids = Arrays.stream(userConfigIds.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).map(Long::valueOf).toArray(Long[]::new);
        Long owner = SecurityUtils.isAdmin() ? null : loginUser.getUserId();
        int rows = sysUserConfigService.deleteSysUserConfigByUserConfigIds(ids, owner);
        return rows > 0 ? AjaxResult.success() : AjaxResult.error("删除用户配置失败");
    }

    @PUT
    public AjaxResult edit(SysUserConfig sysUserConfig) {
        LoginUser loginUser = requireLoginUser();
        int rows = sysUserConfigService.updateSysUserConfig(loginUser.getUserId(), sysUserConfig);
        permissionService.updateRoleAndPermissionOfCurrentUser(loginUser);
        return rows > 0 ? AjaxResult.success() : AjaxResult.error("更新用户配置失败");
    }

    private LoginUser requireLoginUser() {
        LoginUser loginUser = authContext.getLoginUser();
        if (loginUser == null) {
            throw new ApiException(401, "未登录或会话已失效");
        }
        return loginUser;
    }
}
