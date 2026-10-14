package com.cat2bug.admin.resource;

import com.cat2bug.common.api.AjaxResult;
import com.cat2bug.common.constant.UserConstants;
import com.cat2bug.common.domain.LoginUser;
import com.cat2bug.common.domain.SysMenu;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.common.util.StringUtils;
import com.cat2bug.framework.security.AuthContext;
import com.cat2bug.framework.security.RequiresPermissions;
import com.cat2bug.system.service.SysMenuService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/system/menu")
@Produces(MediaType.APPLICATION_JSON)
public class MenuResource {

    @Inject
    AuthContext authContext;

    @Inject
    SysMenuService sysMenuService;

    @GET
    @Path("/list")
    @RequiresPermissions("system:menu:list")
    public AjaxResult list(@QueryParam("menuName") String menuName,
                           @QueryParam("visible") String visible,
                           @QueryParam("status") String status) {
        SysMenu query = new SysMenu();
        query.setMenuName(menuName);
        query.setVisible(visible);
        query.setStatus(status);
        return AjaxResult.success(sysMenuService.selectMenuList(query, requireUserId()));
    }

    @GET
    @Path("/treeselect")
    public AjaxResult treeselect(@QueryParam("menuName") String menuName,
                                 @QueryParam("visible") String visible,
                                 @QueryParam("status") String status) {
        SysMenu query = new SysMenu();
        query.setMenuName(menuName);
        query.setVisible(visible);
        query.setStatus(status);
        return AjaxResult.success(sysMenuService.buildMenuTreeSelect(
                sysMenuService.selectMenuList(query, requireUserId())));
    }

    @GET
    @Path("/roleMenuTreeselect/{roleId}")
    public AjaxResult roleMenuTreeselect(@PathParam("roleId") Long roleId) {
        AjaxResult ajax = AjaxResult.success();
        ajax.put("checkedKeys", sysMenuService.selectMenuListByRoleId(roleId));
        ajax.put("menus", sysMenuService.buildMenuTreeSelect(sysMenuService.selectMenuList(requireUserId())));
        return ajax;
    }

    @GET
    @Path("{menuId}")
    @RequiresPermissions("system:menu:query")
    public AjaxResult getInfo(@PathParam("menuId") Long menuId) {
        return AjaxResult.success(sysMenuService.selectMenuById(menuId));
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("system:menu:add")
    public AjaxResult add(SysMenu menu) {
        if (!sysMenuService.checkMenuNameUnique(menu)) {
            return AjaxResult.error("新增菜单'" + menu.getMenuName() + "'失败，菜单名称已存在");
        }
        if (UserConstants.YES_FRAME.equals(menu.getIsFrame()) && !StringUtils.ishttp(menu.getPath())) {
            return AjaxResult.error("新增菜单'" + menu.getMenuName() + "'失败，地址必须以http(s)://开头");
        }
        menu.setCreateBy(requireLoginUser().getUsername());
        return toAjax(sysMenuService.insertMenu(menu));
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("system:menu:edit")
    public AjaxResult edit(SysMenu menu) {
        if (!sysMenuService.checkMenuNameUnique(menu)) {
            return AjaxResult.error("修改菜单'" + menu.getMenuName() + "'失败，菜单名称已存在");
        }
        if (UserConstants.YES_FRAME.equals(menu.getIsFrame()) && !StringUtils.ishttp(menu.getPath())) {
            return AjaxResult.error("修改菜单'" + menu.getMenuName() + "'失败，地址必须以http(s)://开头");
        }
        if (menu.getMenuId() != null && menu.getMenuId().equals(menu.getParentId())) {
            return AjaxResult.error("修改菜单'" + menu.getMenuName() + "'失败，上级菜单不能选择自己");
        }
        menu.setUpdateBy(requireLoginUser().getUsername());
        return toAjax(sysMenuService.updateMenu(menu));
    }

    @DELETE
    @Path("/{menuId}")
    @RequiresPermissions("system:menu:remove")
    public AjaxResult remove(@PathParam("menuId") Long menuId) {
        if (sysMenuService.hasChildByMenuId(menuId)) {
            return AjaxResult.warn("存在子菜单,不允许删除");
        }
        if (sysMenuService.checkMenuExistRole(menuId)) {
            return AjaxResult.warn("菜单已分配,不允许删除");
        }
        return toAjax(sysMenuService.deleteMenuById(menuId));
    }

    private LoginUser requireLoginUser() {
        LoginUser loginUser = authContext.getLoginUser();
        if (loginUser == null) {
            throw new ApiException(401, "未登录或会话已失效");
        }
        return loginUser;
    }

    private Long requireUserId() {
        return requireLoginUser().getUserId();
    }

    private AjaxResult toAjax(int rows) {
        return rows > 0 ? AjaxResult.success() : AjaxResult.error("操作失败");
    }
}
