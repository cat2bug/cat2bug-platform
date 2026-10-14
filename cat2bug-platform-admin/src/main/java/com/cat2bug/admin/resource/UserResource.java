package com.cat2bug.admin.resource;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.cat2bug.admin.service.UserExcelExportService;
import com.cat2bug.admin.service.UserExcelImportService;
import com.cat2bug.admin.service.UserImportService;
import com.cat2bug.admin.service.UserImportTemplateService;
import com.cat2bug.common.api.AjaxResult;
import com.cat2bug.common.api.TableDataInfo;
import com.cat2bug.common.domain.LoginUser;
import com.cat2bug.common.domain.SysDept;
import com.cat2bug.common.domain.SysRole;
import com.cat2bug.common.domain.SysUser;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.common.util.StringUtils;
import com.cat2bug.framework.security.AuthContext;
import com.cat2bug.framework.security.RequiresPermissions;
import com.cat2bug.system.service.SysDeptService;
import com.cat2bug.system.service.SysPostService;
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
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Path("/system/user")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

    private static final String SPREADSHEET_MEDIA_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Inject
    SysUserService sysUserService;

    @Inject
    SysRoleService sysRoleService;

    @Inject
    SysPostService sysPostService;

    @Inject
    SysDeptService sysDeptService;

    @Inject
    AuthContext authContext;

    @Inject
    UserExcelExportService userExcelExportService;

    @Inject
    UserExcelImportService userExcelImportService;

    @Inject
    UserImportTemplateService userImportTemplateService;

    @Inject
    UserImportService userImportService;

    @GET
    @Path("/list")
    @RequiresPermissions(value = {
            "system:user:list",
            "system:team:member:invite",
            "system:team:member:create",
            "system:project:member"
    }, logical = RequiresPermissions.Logical.OR)
    public TableDataInfo list(@QueryParam("userName") String userName,
                              @QueryParam("status") String status,
                              @QueryParam("phonenumber") String phonenumber,
                              @QueryParam("deptId") Long deptId,
                              @QueryParam("pageNum") Integer pageNum,
                              @QueryParam("pageSize") Integer pageSize) {
        SysUser query = new SysUser();
        query.setUserName(userName);
        query.setStatus(status);
        query.setPhoneNumber(phonenumber);
        query.setDeptId(deptId);
        return sysUserService.selectUserPage(query, pageNum, pageSize);
    }

    @GET
    @RequiresPermissions("system:user:query")
    public AjaxResult getInfoForAdd() {
        return buildUserInfo(null);
    }

    @GET
    @Path("{userId}")
    @RequiresPermissions("system:user:query")
    public AjaxResult getInfo(@PathParam("userId") Long userId) {
        sysUserService.checkUserDataScope(userId);
        return buildUserInfo(userId);
    }

    @POST
    @RequiresPermissions("system:user:add")
    public AjaxResult add(SysUser user) {
        user.setPhoneNumber(StringUtils.trimToNull(user.getPhoneNumber()));
        user.setEmail(StringUtils.trimToNull(user.getEmail()));
        if (!sysUserService.checkUserNameUnique(user)) {
            return AjaxResult.error("新增用户'" + user.getUserName() + "'失败，登录账号已存在");
        }
        if (StringUtils.isNotEmpty(user.getPhoneNumber()) && !sysUserService.checkPhoneUnique(user)) {
            return AjaxResult.error("新增用户'" + user.getUserName() + "'失败，手机号码已存在");
        }
        if (StringUtils.isNotEmpty(user.getEmail()) && !sysUserService.checkEmailUnique(user)) {
            return AjaxResult.error("新增用户'" + user.getUserName() + "'失败，邮箱账号已存在");
        }
        LoginUser loginUser = requireLoginUser();
        user.setCreateBy(loginUser.getUsername());
        user.setPassword(BCrypt.withDefaults().hashToString(12, user.getPassword().toCharArray()));
        return toAjax(sysUserService.insertUser(user));
    }

    @PUT
    @RequiresPermissions("system:user:edit")
    public AjaxResult edit(SysUser user) {
        user.setPhoneNumber(StringUtils.trimToNull(user.getPhoneNumber()));
        user.setEmail(StringUtils.trimToNull(user.getEmail()));
        sysUserService.checkUserAllowed(user);
        LoginUser loginUser = requireLoginUser();
        sysUserService.checkUserDataScope(user.getUserId(), loginUser.getUserId());
        if (!sysUserService.checkUserNameUnique(user)) {
            return AjaxResult.error("修改用户'" + user.getUserName() + "'失败，登录账号已存在");
        }
        if (StringUtils.isNotEmpty(user.getPhoneNumber()) && !sysUserService.checkPhoneUnique(user)) {
            return AjaxResult.error("修改用户'" + user.getUserName() + "'失败，手机号码已存在");
        }
        if (StringUtils.isNotEmpty(user.getEmail()) && !sysUserService.checkEmailUnique(user)) {
            return AjaxResult.error("修改用户'" + user.getUserName() + "'失败，邮箱账号已存在");
        }
        user.setUpdateBy(loginUser.getUsername());
        return toAjax(sysUserService.updateUser(user));
    }

    @DELETE
    @Path("/{userIds}")
    @RequiresPermissions("system:user:remove")
    public AjaxResult remove(@PathParam("userIds") String userIds) {
        Long[] ids = Arrays.stream(userIds.split(","))
                .map(String::trim)
                .map(Long::valueOf)
                .toArray(Long[]::new);
        LoginUser loginUser = requireLoginUser();
        if (Arrays.asList(ids).contains(loginUser.getUserId())) {
            return AjaxResult.error("当前用户不能删除");
        }
        return toAjax(sysUserService.deleteUserByIds(ids));
    }

    @PUT
    @Path("/resetPwd")
    @RequiresPermissions("system:user:resetPwd")
    public AjaxResult resetPwd(SysUser user) {
        sysUserService.checkUserAllowed(user);
        LoginUser loginUser = requireLoginUser();
        sysUserService.checkUserDataScope(user.getUserId(), loginUser.getUserId());
        user.setPassword(BCrypt.withDefaults().hashToString(12, user.getPassword().toCharArray()));
        user.setUpdateBy(loginUser.getUsername());
        return toAjax(sysUserService.resetPwd(user));
    }

    @PUT
    @Path("/changeStatus")
    @RequiresPermissions("system:user:edit")
    public AjaxResult changeStatus(SysUser user) {
        sysUserService.checkUserAllowed(user);
        LoginUser loginUser = requireLoginUser();
        sysUserService.checkUserDataScope(user.getUserId(), loginUser.getUserId());
        user.setUpdateBy(loginUser.getUsername());
        return toAjax(sysUserService.updateUserStatus(user));
    }

    @GET
    @Path("/authRole/{userId}")
    @RequiresPermissions("system:user:query")
    public AjaxResult authRole(@PathParam("userId") Long userId) {
        AjaxResult ajax = AjaxResult.success();
        SysUser user = sysUserService.selectUserById(userId);
        List<SysRole> roles = sysRoleService.selectRolesByUserId(userId);
        ajax.put("user", user);
        ajax.put("roles", SysUser.isAdmin(userId)
                ? roles
                : roles.stream().filter(r -> !r.isAdmin()).collect(Collectors.toList()));
        return ajax;
    }

    @PUT
    @Path("/authRole")
    @RequiresPermissions("system:user:edit")
    public AjaxResult insertAuthRole(@QueryParam("userId") Long userId,
                                     @QueryParam("roleIds") List<Long> roleIds) {
        LoginUser loginUser = requireLoginUser();
        sysUserService.checkUserDataScope(userId, loginUser.getUserId());
        Long[] ids = roleIds == null ? new Long[0] : roleIds.toArray(Long[]::new);
        sysUserService.insertUserAuth(userId, ids);
        return AjaxResult.success();
    }

    @GET
    @Path("/deptTree")
    @RequiresPermissions("system:user:list")
    public AjaxResult deptTree(@QueryParam("deptName") String deptName,
                               @QueryParam("status") String status) {
        SysDept query = new SysDept();
        query.setDeptName(deptName);
        query.setStatus(status);
        return AjaxResult.success(sysDeptService.selectDeptTreeList(query));
    }

    @POST
    @Path("/export")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(SPREADSHEET_MEDIA_TYPE)
    @RequiresPermissions(value = {"system:user:export", "system:member:export"},
            logical = RequiresPermissions.Logical.OR)
    public Response export(@RestForm("userName") String userName,
                           @RestForm("status") String status,
                           @RestForm("phonenumber") String phonenumber,
                           @RestForm("deptId") Long deptId) {
        return buildExportResponse(userName, status, phonenumber, deptId);
    }

    @POST
    @Path("/importTemplate")
    @Consumes(MediaType.WILDCARD)
    @Produces(SPREADSHEET_MEDIA_TYPE)
    public Response importTemplate() {
        byte[] workbook = userImportTemplateService.buildTemplateWorkbook();
        return Response.ok(workbook).type(SPREADSHEET_MEDIA_TYPE).build();
    }

    @POST
    @Path("/importData")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @RequiresPermissions(value = {"system:user:import", "system:member:import"},
            logical = RequiresPermissions.Logical.OR)
    public AjaxResult importData(@RestForm("file") FileUpload file,
                                 @RestForm("updateSupport") Boolean updateSupport) throws IOException {
        if (file == null || file.uploadedFile() == null) {
            return AjaxResult.error("上传文件不能为空");
        }
        LoginUser loginUser = requireLoginUser();
        try (var input = Files.newInputStream(file.uploadedFile())) {
            var users = userExcelImportService.parseWorkbook(input);
            String message = userImportService.importUsers(
                    users,
                    Boolean.TRUE.equals(updateSupport),
                    loginUser.getUsername(),
                    loginUser.getUserId());
            return AjaxResult.success(message);
        }
    }

    Response buildExportResponse(String userName, String status, String phonenumber, Long deptId) {
        SysUser query = new SysUser();
        query.setUserName(userName);
        query.setStatus(status);
        query.setPhoneNumber(phonenumber);
        query.setDeptId(deptId);
        byte[] workbook = userExcelExportService.buildExportWorkbook(query);
        return Response.ok(workbook).type(SPREADSHEET_MEDIA_TYPE).build();
    }

    private AjaxResult buildUserInfo(Long userId) {
        List<SysRole> roles = sysRoleService.selectRoleAll();
        List<SysRole> visibleRoles = SysUser.isAdmin(userId)
                ? roles
                : roles.stream().filter(r -> !r.isAdmin()).collect(Collectors.toList());

        AjaxResult ajax = AjaxResult.success();
        ajax.put("roles", visibleRoles);
        ajax.put("posts", sysPostService.selectPostAll());
        if (userId != null) {
            SysUser sysUser = sysUserService.selectUserById(userId);
            ajax.put(AjaxResult.DATA_TAG, sysUser);
            if (sysUser != null && sysUser.getRoles() != null) {
                ajax.put("roleIds", sysUser.getRoles().stream()
                        .map(SysRole::getRoleId)
                        .collect(Collectors.toList()));
            } else {
                ajax.put("roleIds", Collections.emptyList());
            }
            ajax.put("postIds", sysPostService.selectPostListByUserId(userId));
        }
        return ajax;
    }

    private LoginUser requireLoginUser() {
        LoginUser loginUser = authContext.getLoginUser();
        if (loginUser == null) {
            throw new ApiException(401, "未登录或会话已失效");
        }
        return loginUser;
    }

    private AjaxResult toAjax(int rows) {
        return rows > 0 ? AjaxResult.success() : AjaxResult.error("操作失败");
    }
}
