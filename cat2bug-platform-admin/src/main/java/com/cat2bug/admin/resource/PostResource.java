package com.cat2bug.admin.resource;

import com.cat2bug.admin.service.SystemExcelExportService;
import com.cat2bug.common.api.AjaxResult;
import com.cat2bug.common.api.TableDataInfo;
import com.cat2bug.common.domain.LoginUser;
import com.cat2bug.common.domain.SysPost;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.framework.security.AuthContext;
import com.cat2bug.framework.security.RequiresPermissions;
import com.cat2bug.system.service.SysPostService;
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

@Path("/system/post")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PostResource {

    static final String SPREADSHEET_MEDIA_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Inject
    AuthContext authContext;

    @Inject
    SysPostService sysPostService;

    @Inject
    SystemExcelExportService systemExcelExportService;

    @GET
    @Path("/list")
    @RequiresPermissions("system:post:list")
    public TableDataInfo list(@QueryParam("postCode") String postCode,
                              @QueryParam("postName") String postName,
                              @QueryParam("status") String status,
                              @QueryParam("pageNum") Integer pageNum,
                              @QueryParam("pageSize") Integer pageSize) {
        SysPost query = new SysPost();
        query.setPostCode(postCode);
        query.setPostName(postName);
        query.setStatus(status);
        return sysPostService.selectPostPage(query, pageNum, pageSize);
    }

    @POST
    @Path("/export")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(SPREADSHEET_MEDIA_TYPE)
    @RequiresPermissions("system:post:export")
    public Response export(@RestForm("postCode") String postCode,
                           @RestForm("postName") String postName,
                           @RestForm("status") String status) {
        SysPost query = new SysPost();
        query.setPostCode(postCode);
        query.setPostName(postName);
        query.setStatus(status);
        byte[] workbook = systemExcelExportService.exportPosts(query);
        return Response.ok(workbook).type(SPREADSHEET_MEDIA_TYPE).build();
    }

    @GET
    @Path("/optionselect")
    public AjaxResult optionselect() {
        return AjaxResult.success(sysPostService.selectPostAll());
    }

    @GET
    @Path("/{postId}")
    @RequiresPermissions("system:post:query")
    public AjaxResult getInfo(@PathParam("postId") Long postId) {
        return AjaxResult.success(sysPostService.selectPostById(postId));
    }

    @POST
    @RequiresPermissions("system:post:add")
    public AjaxResult add(SysPost post) {
        if (!sysPostService.checkPostNameUnique(post)) {
            return AjaxResult.error("新增岗位'" + post.getPostName() + "'失败，岗位名称已存在");
        }
        if (!sysPostService.checkPostCodeUnique(post)) {
            return AjaxResult.error("新增岗位'" + post.getPostName() + "'失败，岗位编码已存在");
        }
        post.setCreateBy(requireLoginUser().getUsername());
        sysPostService.insertPost(post);
        return AjaxResult.success(post);
    }

    @PUT
    @RequiresPermissions("system:post:edit")
    public AjaxResult edit(SysPost post) {
        if (!sysPostService.checkPostNameUnique(post)) {
            return AjaxResult.error("修改岗位'" + post.getPostName() + "'失败，岗位名称已存在");
        }
        if (!sysPostService.checkPostCodeUnique(post)) {
            return AjaxResult.error("修改岗位'" + post.getPostName() + "'失败，岗位编码已存在");
        }
        post.setUpdateBy(requireLoginUser().getUsername());
        return toAjax(sysPostService.updatePost(post));
    }

    @DELETE
    @Path("/{postIds}")
    @RequiresPermissions("system:post:remove")
    public AjaxResult remove(@PathParam("postIds") String postIds) {
        Long[] ids = Arrays.stream(postIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .toArray(Long[]::new);
        return toAjax(sysPostService.deletePostByIds(ids));
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
