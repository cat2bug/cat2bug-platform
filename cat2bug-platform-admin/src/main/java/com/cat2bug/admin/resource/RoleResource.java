package com.cat2bug.admin.resource;

import com.cat2bug.admin.service.SystemExcelExportService;
import com.cat2bug.common.api.AjaxResult;
import com.cat2bug.common.api.TableDataInfo;
import com.cat2bug.common.domain.LoginUser;
import com.cat2bug.common.domain.SysDept;
import com.cat2bug.common.domain.SysRole;
import com.cat2bug.common.domain.SysUser;
import com.cat2bug.common.domain.SysUserRole;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.framework.security.AuthContext;
import com.cat2bug.framework.security.RequiresPermissions;
import com.cat2bug.system.service.SysDeptService;
import com.cat2bug.system.service.SysRoleService;
import com.cat2bug.system.service.SysUserService;
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
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestForm;

import java.util.Arrays;
import java.util.List;

@Path("/system/role")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoleResource {

    static final String SPREADSHEET_MEDIA_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Inject
    AuthContext authContext;

    @Inject
    SysRoleService sysRoleService;

    @Inject
    SysUserService sysUserService;

    @Inject
    SysDeptService sysDeptService;

    @Inject
    SystemExcelExportService systemExcelExportService;

    @GET
    @Path("/list")
    @RequiresPermissions("system:role:list")
    public TableDataInfo list(@QueryParam("roleName") String roleName,
                              @QueryParam("roleKey") String roleKey,
                              @QueryParam("status") String status,
                              @QueryParam("pageNum") Integer pageNum,
                              @QueryParam("pageSize") Integer pageSize) {
        SysRole query = new SysRole();
        query.setRoleName(roleName);
        query.setRoleKey(roleKey);
        query.setStatus(status);
        return sysRoleService.selectRolePage(query, pageNum, pageSize);
    }

    @POST
    @Path("/export")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(SPREADSHEET_MEDIA_TYPE)
    @RequiresPermissions("system:role:export")
    public Response export(@RestForm("roleName") String roleName,
                           @RestForm("roleKey") String roleKey,
                           @RestForm("status") String status) {
        SysRole query = new SysRole();
        query.setRoleName(roleName);
        query.setRoleKey(roleKey);
        query.setStatus(status);
        byte[] workbook = systemExcelExportService.exportRoles(query);
        return Response.ok(workbook).type(SPREADSHEET_MEDIA_TYPE).build();
    }

    @GET
    @Path("/optionselect")
    @RequiresPermissions("system:role:query")
    public AjaxResult optionselect() {
        return AjaxResult.success(sysRoleService.selectRoleAll());
    }

    @GET
    @Path("{roleId}")
    @RequiresPermissions("system:role:query")
    public AjaxResult getInfo(@PathParam("roleId") Long roleId) {
        sysRoleService.checkRoleDataScope(roleId);
        return AjaxResult.success(sysRoleService.selectRoleById(roleId));
    }

    @POST
    @RequiresPermissions("system:role:add")
    public AjaxResult add(SysRole role) {
        if (!sysRoleService.checkRoleNameUnique(role)) {
            return AjaxResult.error("新增角色'" + role.getRoleName() + "'失败，角色名称已存在");
        }
        if (!sysRoleService.checkRoleKeyUnique(role)) {
            return AjaxResult.error("新增角色'" + role.getRoleName() + "'失败，角色权限已存在");
        }
        role.setCreateBy(requireLoginUser().getUsername());
        return toAjax(sysRoleService.insertRole(role));
    }

    @PUT
    @RequiresPermissions("system:role:edit")
    public AjaxResult edit(SysRole role) {
        sysRoleService.checkRoleAllowed(role);
        sysRoleService.checkRoleDataScope(role.getRoleId());
        if (!sysRoleService.checkRoleNameUnique(role)) {
            return AjaxResult.error("修改角色'" + role.getRoleName() + "'失败，角色名称已存在");
        }
        if (!sysRoleService.checkRoleKeyUnique(role)) {
            return AjaxResult.error("修改角色'" + role.getRoleName() + "'失败，角色权限已存在");
        }
        role.setUpdateBy(requireLoginUser().getUsername());
        return toAjax(sysRoleService.updateRole(role));
    }

    @PUT
    @Path("/dataScope")
    @RequiresPermissions("system:role:edit")
    public AjaxResult dataScope(SysRole role) {
        sysRoleService.checkRoleAllowed(role);
        sysRoleService.checkRoleDataScope(role.getRoleId());
        return toAjax(sysRoleService.authDataScope(role));
    }

    @PUT
    @Path("/changeStatus")
    @RequiresPermissions("system:role:edit")
    public AjaxResult changeStatus(SysRole role) {
        sysRoleService.checkRoleAllowed(role);
        sysRoleService.checkRoleDataScope(role.getRoleId());
        role.setUpdateBy(requireLoginUser().getUsername());
        return toAjax(sysRoleService.updateRoleStatus(role));
    }

    @DELETE
    @Path("/{roleIds}")
    @RequiresPermissions("system:role:remove")
    public AjaxResult remove(@PathParam("roleIds") String roleIds) {
        Long[] ids = parseIds(roleIds);
        return toAjax(sysRoleService.deleteRoleByIds(ids));
    }

    @GET
    @Path("/authUser/allocatedList")
    @RequiresPermissions("system:role:list")
    public TableDataInfo allocatedList(@QueryParam("roleId") Long roleId,
                                       @QueryParam("userName") String userName,
                                       @QueryParam("phonenumber") String phonenumber,
                                       @QueryParam("pageNum") Integer pageNum,
                                       @QueryParam("pageSize") Integer pageSize) {
        SysUser query = new SysUser();
        query.setRoleId(roleId);
        query.setUserName(userName);
        query.setPhoneNumber(phonenumber);
        return sysUserService.selectAllocatedPage(query, pageNum, pageSize);
    }

    @GET
    @Path("/authUser/unallocatedList")
    @RequiresPermissions("system:role:list")
    public TableDataInfo unallocatedList(@QueryParam("roleId") Long roleId,
                                           @QueryParam("userName") String userName,
                                           @QueryParam("phonenumber") String phonenumber,
                                           @QueryParam("pageNum") Integer pageNum,
                                           @QueryParam("pageSize") Integer pageSize) {
        SysUser query = new SysUser();
        query.setRoleId(roleId);
        query.setUserName(userName);
        query.setPhoneNumber(phonenumber);
        return sysUserService.selectUnallocatedPage(query, pageNum, pageSize);
    }

    @PUT
    @Path("/authUser/cancel")
    @RequiresPermissions("system:role:edit")
    public AjaxResult cancelAuthUser(SysUserRole userRole) {
        return toAjax(sysRoleService.deleteAuthUser(userRole));
    }

    @PUT
    @Path("/authUser/cancelAll")
    @RequiresPermissions("system:role:edit")
    public AjaxResult cancelAuthUserAll(@QueryParam("roleId") Long roleId,
                                        @QueryParam("userIds") List<Long> userIds) {
        return toAjax(sysRoleService.deleteAuthUsers(roleId, toArray(userIds)));
    }

    @PUT
    @Path("/authUser/selectAll")
    @RequiresPermissions("system:role:edit")
    public AjaxResult selectAuthUserAll(@QueryParam("roleId") Long roleId,
                                        @QueryParam("userIds") List<Long> userIds) {
        sysRoleService.checkRoleDataScope(roleId);
        return toAjax(sysRoleService.insertAuthUsers(roleId, toArray(userIds)));
    }

    @GET
    @Path("/deptTree/{roleId}")
    @RequiresPermissions("system:role:query")
    public AjaxResult deptTree(@PathParam("roleId") Long roleId) {
        AjaxResult ajax = AjaxResult.success();
        ajax.put("checkedKeys", sysRoleService.selectDeptListByRoleId(roleId));
        ajax.put("depts", sysDeptService.selectDeptTreeList(new SysDept()));
        return ajax;
    }

    private LoginUser requireLoginUser() {
        LoginUser loginUser = authContext.getLoginUser();
        if (loginUser == null) {
            throw new ApiException(401, "未登录或会话已失效");
        }
        return loginUser;
    }

    private static Long[] parseIds(String ids) {
        return Arrays.stream(ids.split(","))
                .map(String::trim)
                .map(Long::valueOf)
                .toArray(Long[]::new);
    }

    private static Long[] toArray(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new Long[0];
        }
        return ids.toArray(Long[]::new);
    }

    private AjaxResult toAjax(int rows) {
        return rows > 0 ? AjaxResult.success() : AjaxResult.error("操作失败");
    }
}
