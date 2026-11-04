package com.cat2bug.admin.resource;

import com.cat2bug.common.api.AjaxResult;
import com.cat2bug.common.domain.LoginUser;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.framework.security.AuthContext;
import com.cat2bug.framework.security.LoginUserResolver;
import com.cat2bug.system.service.ProjectMembershipAuth;
import com.cat2bug.system.service.SysModulePageSettingsService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

/**
 * 项目成员只读所属团队的模块页面设置，供布局 / 工作台 bootstrap。
 * 不绑定单一业务权限，避免关闭某模块后成员失去读取闸门配置的能力。
 */
@Path("/system/project/{projectId}/module-page-settings")
@Produces(MediaType.APPLICATION_JSON)
public class ProjectModulePageSettingsResource {

    @Inject
    SysModulePageSettingsService sysModulePageSettingsService;

    @Inject
    ProjectMembershipAuth projectMembershipAuth;

    @Inject
    LoginUserResolver loginUserResolver;

    @Inject
    AuthContext authContext;

    @GET
    public AjaxResult get(@PathParam("projectId") Long projectId, @Context ContainerRequestContext request) {
        LoginUser loginUser = loginUserResolver.resolve(request);
        if (loginUser == null) {
            loginUser = authContext.getLoginUser();
        }
        if (loginUser == null || loginUser.getUserId() == null) {
            throw new ApiException(401, "未登录或会话已失效");
        }
        projectMembershipAuth.requireProjectAccess(loginUser.getUserId(), projectId);
        return AjaxResult.success(sysModulePageSettingsService.viewByProjectId(projectId));
    }
}
