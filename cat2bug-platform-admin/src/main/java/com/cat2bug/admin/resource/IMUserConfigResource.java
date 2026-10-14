package com.cat2bug.admin.resource;

import com.cat2bug.common.api.AjaxResult;
import com.cat2bug.common.domain.LoginUser;
import com.cat2bug.common.domain.im.IMUserConfig;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.common.util.ImNoticeDefaults;
import com.cat2bug.framework.security.AuthContext;
import com.cat2bug.framework.security.RequiresPermissions;
import com.cat2bug.system.service.ImUserConfigService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/im/config")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class IMUserConfigResource {

    @Inject
    AuthContext authContext;

    @Inject
    ImUserConfigService imUserConfigService;

    @GET
    @RequiresPermissions("notice:list")
    public AjaxResult getInfo(@QueryParam("projectId") Long projectId,
                              @QueryParam("memberId") Long memberId) {
        LoginUser loginUser = requireLoginUser();
        return AjaxResult.success(imUserConfigService.selectImUserConfigByProjectAndMember(
                projectId, memberId, ImNoticeDefaults.defaultDefectNoticeOption(), loginUser));
    }

    @POST
    @RequiresPermissions("notice:list")
    public AjaxResult save(IMUserConfig imUserConfig) {
        requireLoginUser();
        return toAjax(imUserConfigService.saveImUserConfig(imUserConfig));
    }

    @DELETE
    @Path("/{imConfigId}")
    @RequiresPermissions("notice:list")
    public AjaxResult remove(@PathParam("imConfigId") Long imConfigId) {
        requireLoginUser();
        return toAjax(imUserConfigService.deleteImUserConfigByImConfigId(imConfigId));
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
