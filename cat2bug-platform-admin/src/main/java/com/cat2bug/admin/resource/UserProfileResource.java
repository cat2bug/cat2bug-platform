package com.cat2bug.admin.resource;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.cat2bug.common.api.AjaxResult;
import com.cat2bug.common.domain.LoginUser;
import com.cat2bug.common.domain.SysUser;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.common.util.StringUtils;
import com.cat2bug.framework.security.AuthContext;
import com.cat2bug.framework.security.TokenService;
import com.cat2bug.framework.storage.FileUploadService;
import com.cat2bug.system.service.SysUserService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.IOException;
import java.nio.file.Files;

@Path("/system/user/profile")
@Produces(MediaType.APPLICATION_JSON)
public class UserProfileResource {

    @Inject
    SysUserService sysUserService;

    @Inject
    AuthContext authContext;

    @Inject
    TokenService tokenService;

    @Inject
    FileUploadService fileUploadService;

    @GET
    public AjaxResult profile() {
        LoginUser loginUser = requireLoginUser();
        SysUser user = loginUser.getUser();
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            SysUser refreshed = sysUserService.selectUserById(user.getUserId());
            if (refreshed != null) {
                user = refreshed;
                loginUser.setUser(user);
            }
        }
        AjaxResult ajax = AjaxResult.success(user);
        ajax.put("roleGroup", sysUserService.selectUserRoleGroup(user));
        ajax.put("postGroup", sysUserService.selectUserPostGroup(user.getUserName()));
        return ajax;
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public AjaxResult updateProfile(SysUser user) {
        LoginUser loginUser = requireLoginUser();
        SysUser currentUser = loginUser.getUser();
        currentUser.setNickName(user.getNickName());
        currentUser.setEmail(StringUtils.trimToNull(user.getEmail()));
        currentUser.setPhoneNumber(StringUtils.trimToNull(user.getPhoneNumber()));
        currentUser.setSex(user.getSex());
        if (StringUtils.isNotEmpty(user.getPhoneNumber()) && !sysUserService.checkPhoneUnique(currentUser)) {
            return AjaxResult.error("修改用户'" + user.getUserName() + "'失败，手机号码已存在");
        }
        if (StringUtils.isNotEmpty(user.getEmail()) && !sysUserService.checkEmailUnique(currentUser)) {
            return AjaxResult.error("修改用户'" + user.getUserName() + "'失败，邮箱账号已存在");
        }
        if (sysUserService.updateUserProfile(currentUser) > 0) {
            tokenService.updateLoginUser(loginUser);
            return AjaxResult.success();
        }
        return AjaxResult.error("修改个人信息异常，请联系管理员");
    }

    @PUT
    @Path("/updatePwd")
    public AjaxResult updatePwd(@QueryParam("oldPassword") String oldPassword,
                                @QueryParam("newPassword") String newPassword) {
        LoginUser loginUser = requireLoginUser();
        String password = loginUser.getUser().getPassword();
        if (!BCrypt.verifyer().verify(oldPassword.toCharArray(), password).verified) {
            return AjaxResult.error("修改密码失败，旧密码错误");
        }
        if (BCrypt.verifyer().verify(newPassword.toCharArray(), password).verified) {
            return AjaxResult.error("新密码不能与旧密码相同");
        }
        String encrypted = BCrypt.withDefaults().hashToString(12, newPassword.toCharArray());
        if (sysUserService.resetUserPwd(loginUser.getUsername(), encrypted) > 0) {
            loginUser.getUser().setPassword(encrypted);
            tokenService.updateLoginUser(loginUser);
            return AjaxResult.success();
        }
        return AjaxResult.error("修改密码异常，请联系管理员");
    }

    @POST
    @Path("/avatar")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public AjaxResult avatar(@RestForm("avatarfile") FileUpload file) throws IOException {
        if (file == null || file.uploadedFile() == null || Files.size(file.uploadedFile()) == 0) {
            return AjaxResult.error("上传图片异常，请联系管理员");
        }
        LoginUser loginUser = requireLoginUser();
        try (var input = Files.newInputStream(file.uploadedFile())) {
            FileUploadService.UploadResult result = fileUploadService.upload(
                    input, Files.size(file.uploadedFile()), file.fileName(), null);
            String avatar = result.getUrl();
            if (sysUserService.updateUserAvatar(loginUser.getUsername(), avatar)) {
                AjaxResult ajax = AjaxResult.success();
                ajax.put("imgUrl", avatar);
                loginUser.getUser().setAvatar(avatar);
                tokenService.updateLoginUser(loginUser);
                return ajax;
            }
        }
        return AjaxResult.error("上传图片异常，请联系管理员");
    }

    private LoginUser requireLoginUser() {
        LoginUser loginUser = authContext.getLoginUser();
        if (loginUser == null) {
            throw new ApiException(401, "未登录或会话已失效");
        }
        return loginUser;
    }
}
