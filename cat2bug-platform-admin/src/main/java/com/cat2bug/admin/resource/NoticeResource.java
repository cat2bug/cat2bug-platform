package com.cat2bug.admin.resource;

import com.cat2bug.admin.service.ImNoticeDispatchService;
import com.cat2bug.common.api.AjaxResult;
import com.cat2bug.common.api.TableDataInfo;
import com.cat2bug.common.domain.LoginUser;
import com.cat2bug.common.domain.SysNotice;
import com.cat2bug.common.domain.vo.SendNotice;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.framework.security.AuthContext;
import com.cat2bug.framework.security.RequiresPermissions;
import com.cat2bug.system.service.SysNoticeService;
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

import java.util.Arrays;

@Path("/system/notice")
@Produces(MediaType.APPLICATION_JSON)
public class NoticeResource {

    @Inject
    AuthContext authContext;

    @Inject
    SysNoticeService sysNoticeService;

    @Inject
    ImNoticeDispatchService imNoticeDispatchService;

    @GET
    @Path("/list")
    @RequiresPermissions("notice:list")
    public TableDataInfo list(@QueryParam("groupName") String groupName,
                              @QueryParam("noticeTitle") String noticeTitle,
                              @QueryParam("noticeType") String noticeType,
                              @QueryParam("pageNum") Integer pageNum,
                              @QueryParam("pageSize") Integer pageSize) {
        LoginUser loginUser = requireLoginUser();
        SysNotice query = new SysNotice();
        query.setReceiverId(loginUser.getUserId());
        query.setGroupName(groupName);
        query.setNoticeTitle(noticeTitle);
        query.setNoticeType(noticeType);
        return sysNoticeService.selectNoticePage(query, pageNum, pageSize);
    }

    @GET
    @Path("/statistics/group")
    @RequiresPermissions("notice:list")
    public TableDataInfo groupStatistics() {
        LoginUser loginUser = requireLoginUser();
        var rows = sysNoticeService.noticeGroupStatistics(loginUser.getUserId());
        return new TableDataInfo(rows, rows.size());
    }

    @GET
    @Path("/{noticeId}")
    @RequiresPermissions("notice:query")
    public AjaxResult getInfo(@PathParam("noticeId") String noticeId) {
        return AjaxResult.success(sysNoticeService.selectNoticeById(noticeId));
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("system:notice:add")
    public AjaxResult add(SysNotice notice) {
        notice.setCreateBy(requireLoginUser().getUsername());
        return toAjax(sysNoticeService.insertNotice(notice));
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("system:notice:edit")
    public AjaxResult edit(SysNotice notice) {
        notice.setUpdateBy(requireLoginUser().getUsername());
        return toAjax(sysNoticeService.updateNotice(notice));
    }

    @DELETE
    @Path("/{noticeIds}")
    @RequiresPermissions("notice:remove")
    public AjaxResult remove(@PathParam("noticeIds") String noticeIds) {
        String[] ids = Arrays.stream(noticeIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
        return toAjax(sysNoticeService.deleteNoticeByIds(ids));
    }

    @POST
    @Path("/send")
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("notice:send")
    public AjaxResult send(SendNotice notice) {
        imNoticeDispatchService.sendNotice(requireLoginUser().getUserId(), notice);
        return AjaxResult.success();
    }

    private AjaxResult toAjax(int rows) {
        return rows > 0 ? AjaxResult.success() : AjaxResult.error("操作失败");
    }

    private LoginUser requireLoginUser() {
        LoginUser loginUser = authContext.getLoginUser();
        if (loginUser == null) {
            throw new ApiException(401, "未登录或会话已失效");
        }
        return loginUser;
    }
}
