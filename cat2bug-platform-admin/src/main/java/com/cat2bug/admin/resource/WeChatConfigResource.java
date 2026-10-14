package com.cat2bug.admin.resource;

import com.cat2bug.common.api.AjaxResult;
import com.cat2bug.common.domain.im.EnterpriseWeChatProjectConfig;
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

@Path("/im/project/wechat/config")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class WeChatConfigResource {

    private static final Logger LOG = Logger.getLogger(WeChatConfigResource.class);

    @Inject
    ImProjectConfigService imProjectConfigService;

    @Inject
    ImConfigTestService imConfigTestService;

    @GET
    @RequiresPermissions("wechat:query")
    public AjaxResult getInfo(@QueryParam("projectId") Long projectId) {
        return AjaxResult.success(imProjectConfigService.selectImProjectConfigByProjectIdAndSystemCode(
                projectId, EnterpriseWeChatProjectConfig.SYSTEM_CODE));
    }

    @POST
    @RequiresPermissions("wechat:save")
    public AjaxResult save(IMProjectConfig imProjectConfig) {
        return toAjax(imProjectConfigService.saveImProjectConfig(
                imProjectConfig, EnterpriseWeChatProjectConfig.SYSTEM_CODE));
    }

    @POST
    @Path("/single-test")
    @RequiresPermissions("wechat:query")
    public AjaxResult singleTest(Map<String, Object> params) {
        try {
            Long projectId = Long.valueOf(params.get("projectId").toString());
            Long memberId = Long.valueOf(params.get("memberId").toString());
            imConfigTestService.sendWeChatSingleTest(projectId, memberId, imConfigTestService.extractConfigMap(params));
            return AjaxResult.success("测试消息已发送到企业微信单发用户");
        } catch (Exception e) {
            LOG.error("企业微信单发测试消息发送失败", e);
            return AjaxResult.error("测试消息发送失败: " + e.getMessage());
        }
    }

    @POST
    @Path("/group-test")
    @RequiresPermissions("wechat:query")
    public AjaxResult groupTest(Map<String, Object> params) {
        try {
            Long projectId = Long.valueOf(params.get("projectId").toString());
            Long memberId = Long.valueOf(params.get("memberId").toString());
            imConfigTestService.sendWeChatGroupTest(projectId, memberId, imConfigTestService.extractConfigMap(params));
            return AjaxResult.success("测试消息已发送到企业微信群机器人");
        } catch (Exception e) {
            LOG.error("企业微信群发测试消息发送失败", e);
            return AjaxResult.error("测试消息发送失败: " + e.getMessage());
        }
    }

    private AjaxResult toAjax(int rows) {
        return rows > 0 ? AjaxResult.success() : AjaxResult.error("操作失败");
    }
}
