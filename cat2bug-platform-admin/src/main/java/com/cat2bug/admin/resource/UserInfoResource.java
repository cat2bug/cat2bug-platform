package com.cat2bug.admin.resource;

import com.cat2bug.common.api.AjaxResult;
import com.cat2bug.common.domain.LoginUser;
import com.cat2bug.common.domain.SysTeam;
import com.cat2bug.common.domain.SysUser;
import com.cat2bug.common.domain.SysUserConfig;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.framework.security.AuthContext;
import com.cat2bug.system.service.SysMenuService;
import com.cat2bug.system.service.SysPermissionService;
import com.cat2bug.system.service.SysTeamService;
import com.cat2bug.system.service.SysUserTeamService;
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

    @Inject
    SysUserTeamService sysUserTeamService;

    @Inject
    SysTeamService sysTeamService;

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
        Long currentTeamId = config != null ? config.getCurrentTeamId() : null;
        boolean formalMember = currentTeamId != null
                && currentTeamId > 0
                && (SysUser.isAdmin(user.getUserId())
                || sysUserTeamService.isFormalMember(currentTeamId, user.getUserId()));

        Map<String, Object> contextTeam = null;
        if (currentTeamId != null && currentTeamId > 0) {
            SysTeam team = sysTeamService.selectSysTeamByTeamId(currentTeamId);
            if (team != null) {
                contextTeam = new HashMap<>();
                contextTeam.put("teamId", team.getTeamId());
                contextTeam.put("teamName", team.getTeamName());
                contextTeam.put("teamIcon", team.getTeamIcon());
                contextTeam.put("introduce", team.getIntroduce());
                contextTeam.put("formalMember", formalMember);
            }
        }

        // 团队角色变更（如转让创建人）后 session 可能仍是旧权限；每次 getInfo 按当前上下文重算
        permissionService.updateRoleAndPermissionOfCurrentUser(loginUser);

        AjaxResult ajax = AjaxResult.success();
        ajax.put("user", safeUser);
        ajax.put("roles", permissionService.getRoleKeys(loginUser.getUser()));
        ajax.put("permissions", loginUser.getPermissions());
        ajax.put("config", config != null ? config : Map.of());
        ajax.put("teamMember", formalMember);
        ajax.put("formalMember", formalMember);
        ajax.put("contextTeam", contextTeam);
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
