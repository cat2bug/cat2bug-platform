package com.cat2bug.admin.resource;

import com.cat2bug.common.api.AjaxResult;
import com.cat2bug.common.domain.im.DingProjectConfig;
import com.cat2bug.common.domain.im.IMProjectConfig;
import com.cat2bug.framework.security.RequiresPermissions;
import com.cat2bug.system.service.ImConfigTestService;
import com.cat2bug.system.service.ImProjectConfigService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.jboss.logging.Logger;

import java.util.Map;

@Path("/im/project/ding/config")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DingConfigResource {

    private static final Logger LOG = Logger.getLogger(DingConfigResource.class);

    @Inject
    ImProjectConfigService imProjectConfigService;

    @Inject
    ImConfigTestService imConfigTestService;

    @GET
    @RequiresPermissions("ding:query")
    public AjaxResult getInfo(@QueryParam("projectId") Long projectId) {
        return AjaxResult.success(imProjectConfigService.selectImProjectConfigByProjectIdAndSystemCode(
                projectId, DingProjectConfig.SYSTEM_CODE));
    }

    @POST
    @RequiresPermissions("ding:save")
    public AjaxResult save(IMProjectConfig imProjectConfig) {
        return toAjax(imProjectConfigService.saveImProjectConfig(imProjectConfig, DingProjectConfig.SYSTEM_CODE));
    }

    @POST
    @Path("/group-test")
    @RequiresPermissions("ding:query")
    public AjaxResult groupTest(Map<String, Object> params) {
        try {
            Long projectId = Long.valueOf(params.get("projectId").toString());
            Long memberId = Long.valueOf(params.get("memberId").toString());
            imConfigTestService.sendDingGroupTest(projectId, memberId, imConfigTestService.extractConfigMap(params));
            return AjaxResult.success("测试消息已发送到钉钉群机器人");
        } catch (Exception e) {
            LOG.error("钉钉群发测试消息发送失败", e);
            return AjaxResult.error("测试消息发送失败: " + e.getMessage());
        }
    }

    @POST
    @Path("/single-test")
    @RequiresPermissions("ding:query")
    public AjaxResult singleTest(Map<String, Object> params) {
        try {
            Long projectId = Long.valueOf(params.get("projectId").toString());
            Long memberId = Long.valueOf(params.get("memberId").toString());
            imConfigTestService.sendDingSingleTest(projectId, memberId, imConfigTestService.extractConfigMap(params));
            return AjaxResult.success("测试消息已发送到钉钉单发用户");
        } catch (Exception e) {
            LOG.error("钉钉单发测试消息发送失败", e);
            return AjaxResult.error("测试消息发送失败: " + e.getMessage());
        }
    }

    private AjaxResult toAjax(int rows) {
        return rows > 0 ? AjaxResult.success() : AjaxResult.error("操作失败");
    }
}
