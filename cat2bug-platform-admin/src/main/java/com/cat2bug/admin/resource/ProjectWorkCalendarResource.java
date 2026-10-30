package com.cat2bug.admin.resource;

import com.cat2bug.common.api.AjaxResult;
import com.cat2bug.common.domain.LoginUser;
import com.cat2bug.common.domain.SysProjectWorkCalendar;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.framework.security.AuthContext;
import com.cat2bug.framework.security.RequiresPermissions;
import com.cat2bug.system.service.ProjectPlanAuth;
import com.cat2bug.system.service.SysProjectWorkCalendarService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 项目工作日历：成员可读（缺陷列表权限），项目创建人/管理员可写。
 */
@Path("/system/project/{projectId:[0-9]+}/work-calendar")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProjectWorkCalendarResource {

    static final String READ_PERM_DEFECT = "system:defect:list";
    static final String READ_PERM_PROJECT = "system:project:query";
    static final String EDIT_PERMISSION = "system:project:edit";

    public record CalendarBody(List<Integer> workWeek, Map<String, String> exceptions) {
    }

    @Inject
    AuthContext authContext;

    @Inject
    SysProjectWorkCalendarService calendarService;

    @Inject
    ProjectPlanAuth projectPlanAuth;

    @GET
    @RequiresPermissions(value = {READ_PERM_DEFECT, READ_PERM_PROJECT},
            logical = RequiresPermissions.Logical.OR)
    public AjaxResult get(@PathParam("projectId") Long projectId) {
        LoginUser loginUser = requireLoginUser();
        return AjaxResult.success(view(projectId, calendarService.getOrDefault(projectId), loginUser));
    }

    @PUT
    @RequiresPermissions(EDIT_PERMISSION)
    public AjaxResult save(@PathParam("projectId") Long projectId, CalendarBody body) {
        LoginUser loginUser = requireLoginUser();
        if (body == null) {
            throw new ApiException(400, "请求体不能为空");
        }
        projectPlanAuth.requireProjectPlanAdmin(projectId, loginUser.getUserId());
        SysProjectWorkCalendar saved = calendarService.save(
                projectId,
                body.workWeek(),
                body.exceptions(),
                String.valueOf(loginUser.getUserId()));
        return AjaxResult.success(view(projectId, saved, loginUser));
    }

    private Map<String, Object> view(Long projectId, SysProjectWorkCalendar cal, LoginUser loginUser) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("projectId", cal.getProjectId());
        out.put("workWeek", cal.getWorkWeek());
        out.put("exceptions", cal.getExceptions());
        out.put("updateBy", cal.getUpdateBy());
        out.put("updateTime", cal.getUpdateTime());
        out.put("editable", projectPlanAuth.isProjectPlanAdmin(projectId, loginUser.getUserId()));
        return out;
    }

    private LoginUser requireLoginUser() {
        LoginUser loginUser = authContext.getLoginUser();
        if (loginUser == null) {
            throw new ApiException(401, "未登录或会话已失效");
        }
        return loginUser;
    }
}
