package com.cat2bug.quarkus.admin.resource;

import com.cat2bug.quarkus.common.api.AjaxResult;
import com.cat2bug.quarkus.common.api.TableDataInfo;
import com.cat2bug.quarkus.common.domain.SysComment;
import com.cat2bug.quarkus.framework.security.RequiresPermissions;
import com.cat2bug.quarkus.system.service.SysCommentService;
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

@Path("/system/comment")
@Produces(MediaType.APPLICATION_JSON)
public class CommentResource {

    @Inject
    SysCommentService sysCommentService;

    @GET
    @Path("/list")
    @RequiresPermissions(value = {"system:comment:list", "system:defect:list", "system:defect:log:list",
            "system:report:list", "system:report:query"},
            logical = RequiresPermissions.Logical.OR)
    public TableDataInfo list(@QueryParam("createById") Long createById,
                              @QueryParam("moduleType") String moduleType,
                              @QueryParam("correlationId") Long correlationId,
                              @QueryParam("parentId") Long parentId,
                              @QueryParam("pageNum") Integer pageNum,
                              @QueryParam("pageSize") Integer pageSize) {
        SysComment query = new SysComment();
        query.setCreateById(createById);
        query.setModuleType(moduleType);
        query.setCorrelationId(correlationId);
        query.setParentId(parentId);
        return sysCommentService.selectCommentPage(query, pageNum, pageSize);
    }

    @GET
    @Path("/{commentId}")
    @RequiresPermissions(value = {"system:comment:query", "system:defect:list", "system:defect:log:list",
            "system:report:list", "system:report:query"},
            logical = RequiresPermissions.Logical.OR)
    public AjaxResult getInfo(@PathParam("commentId") Long commentId) {
        return AjaxResult.success(sysCommentService.selectSysCommentByCommentId(commentId));
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value = {"system:comment:add", "system:defect:list", "system:defect:log:list",
            "system:report:list", "system:report:query"},
            logical = RequiresPermissions.Logical.OR)
    public AjaxResult add(SysComment sysComment) {
        int rows = sysCommentService.insertSysComment(sysComment);
        return rows > 0 ? AjaxResult.success() : AjaxResult.error("新增失败");
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value = {"system:comment:edit", "system:defect:list", "system:defect:log:list"},
            logical = RequiresPermissions.Logical.OR)
    public AjaxResult edit(SysComment sysComment) {
        int rows = sysCommentService.updateSysComment(sysComment);
        return rows > 0 ? AjaxResult.success() : AjaxResult.error("更新失败");
    }

    @DELETE
    @Path("/{commentIds}")
    @RequiresPermissions(value = {"system:comment:remove", "system:defect:list", "system:defect:log:list",
            "system:report:list", "system:report:query"},
            logical = RequiresPermissions.Logical.OR)
    public AjaxResult remove(@PathParam("commentIds") String commentIds) {
        Long[] ids = parseIds(commentIds);
        int rows = sysCommentService.deleteSysCommentByCommentIds(ids);
        return rows > 0 ? AjaxResult.success() : AjaxResult.error("删除失败");
    }

    private static Long[] parseIds(String commentIds) {
        String[] parts = commentIds.split(",");
        Long[] ids = new Long[parts.length];
        for (int i = 0; i < parts.length; i++) {
            ids[i] = Long.parseLong(parts[i].trim());
        }
        return ids;
    }
}
