package com.cat2bug.admin.resource;

import com.cat2bug.common.api.AjaxResult;
import com.cat2bug.common.constant.UserConstants;
import com.cat2bug.common.domain.LoginUser;
import com.cat2bug.common.domain.SysDept;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.common.util.StringUtils;
import com.cat2bug.framework.security.RequiresPermissions;
import com.cat2bug.framework.security.AuthContext;
import com.cat2bug.system.service.SysDeptService;
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
import java.util.List;

@Path("/system/dept")
@Produces(MediaType.APPLICATION_JSON)
public class DeptResource {

    @Inject
    AuthContext authContext;

    @Inject
    SysDeptService sysDeptService;

    @GET
    @Path("/list")
    @RequiresPermissions("system:dept:list")
    public AjaxResult list(@QueryParam("deptName") String deptName,
                           @QueryParam("status") String status) {
        SysDept query = new SysDept();
        query.setDeptName(deptName);
        query.setStatus(status);
        return AjaxResult.success(sysDeptService.selectDeptList(query));
    }

    @GET
    @Path("/list/exclude/{deptId}")
    @RequiresPermissions("system:dept:list")
    public AjaxResult excludeChild(@PathParam("deptId") Long deptId) {
        List<SysDept> depts = sysDeptService.selectDeptList(new SysDept());
        depts.removeIf(d -> isExcludedDept(d, deptId));
        return AjaxResult.success(depts);
    }

    @GET
    @Path("/treeselect")
    @RequiresPermissions("system:dept:list")
    public AjaxResult treeselect(@QueryParam("deptName") String deptName,
                                 @QueryParam("status") String status) {
        SysDept query = new SysDept();
        query.setDeptName(deptName);
        query.setStatus(status);
        return AjaxResult.success(sysDeptService.selectDeptTreeList(query));
    }

    @GET
    @Path("/{deptId}")
    @RequiresPermissions("system:dept:query")
    public AjaxResult getInfo(@PathParam("deptId") Long deptId) {
        sysDeptService.checkDeptDataScope(deptId);
        return AjaxResult.success(sysDeptService.selectDeptById(deptId));
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("system:dept:add")
    public AjaxResult add(SysDept dept) {
        if (!sysDeptService.checkDeptNameUnique(dept)) {
            return AjaxResult.error("新增部门'" + dept.getDeptName() + "'失败，部门名称已存在");
        }
        dept.setCreateBy(requireLoginUser().getUsername());
        return toAjax(sysDeptService.insertDept(dept));
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("system:dept:edit")
    public AjaxResult edit(SysDept dept) {
        sysDeptService.checkDeptDataScope(dept.getDeptId());
        if (!sysDeptService.checkDeptNameUnique(dept)) {
            return AjaxResult.error("修改部门'" + dept.getDeptName() + "'失败，部门名称已存在");
        }
        if (dept.getDeptId() != null && dept.getDeptId().equals(dept.getParentId())) {
            return AjaxResult.error("修改部门'" + dept.getDeptName() + "'失败，上级部门不能是自己");
        }
        if (StringUtils.equals(UserConstants.DEPT_DISABLE, dept.getStatus())
                && sysDeptService.selectNormalChildrenDeptById(dept.getDeptId()) > 0) {
            return AjaxResult.error("该部门包含未停用的子部门！");
        }
        dept.setUpdateBy(requireLoginUser().getUsername());
        return toAjax(sysDeptService.updateDept(dept));
    }

    @DELETE
    @Path("/{deptId}")
    @RequiresPermissions("system:dept:remove")
    public AjaxResult remove(@PathParam("deptId") Long deptId) {
        if (sysDeptService.hasChildByDeptId(deptId)) {
            return AjaxResult.warn("存在下级部门,不允许删除");
        }
        if (sysDeptService.checkDeptExistUser(deptId)) {
            return AjaxResult.warn("部门存在用户,不允许删除");
        }
        sysDeptService.checkDeptDataScope(deptId);
        return toAjax(sysDeptService.deleteDeptById(deptId));
    }

    private static boolean isExcludedDept(SysDept dept, Long deptId) {
        if (dept.getDeptId() != null && dept.getDeptId().equals(deptId)) {
            return true;
        }
        String[] ancestors = StringUtils.split(dept.getAncestors(), ",");
        return Arrays.asList(ancestors).contains(String.valueOf(deptId));
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
