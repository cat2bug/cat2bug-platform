package com.cat2bug.admin.resource;

import com.cat2bug.admin.setup.UpgradeInstallService;
import com.cat2bug.admin.setup.UpgradePreflightService;
import com.cat2bug.admin.setup.UpgradeService;
import com.cat2bug.common.api.AjaxResult;
import com.cat2bug.common.domain.setup.SetupSubmitRequest;
import com.cat2bug.common.exception.ApiException;
import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.jboss.logging.Logger;

import java.util.Map;

@Path("/upgrade")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UpgradeResource {

    private static final Logger LOG = Logger.getLogger(UpgradeResource.class);

    @Inject
    UpgradeService upgradeService;

    @Inject
    UpgradePreflightService upgradePreflightService;

    @Inject
    UpgradeInstallService upgradeInstallService;

    @GET
    @Path("status")
    public AjaxResult status() {
        return AjaxResult.success(upgradeService.getStatus());
    }

    @GET
    @Path("preflight")
    public AjaxResult preflight() {
        return AjaxResult.success(upgradePreflightService.buildPreflight());
    }

    @POST
    @Path("submit")
    @Blocking
    public AjaxResult submit(SetupSubmitRequest request) {
        try {
            Map<String, Object> data = upgradeInstallService.submit(request, false);
            return AjaxResult.success(String.valueOf(data.get("message")), data);
        } catch (ApiException e) {
            return AjaxResult.error(e.getMessage());
        } catch (Exception e) {
            LOG.error("升级提交失败", e);
            return AjaxResult.error(rootMessage(e));
        }
    }

    @POST
    @Path("retry")
    @Blocking
    public AjaxResult retry(SetupSubmitRequest request) {
        try {
            Map<String, Object> data = upgradeInstallService.submit(request, true);
            return AjaxResult.success(String.valueOf(data.get("message")), data);
        } catch (ApiException e) {
            return AjaxResult.error(e.getMessage());
        } catch (Exception e) {
            LOG.error("升级重试失败", e);
            return AjaxResult.error(rootMessage(e));
        }
    }

    @POST
    @Path("rollback")
    @Blocking
    public AjaxResult rollback(SetupSubmitRequest request) {
        try {
            Map<String, Object> data = upgradeInstallService.rollback(request);
            return AjaxResult.success(String.valueOf(data.get("message")), data);
        } catch (ApiException e) {
            return AjaxResult.error(e.getMessage());
        } catch (Exception e) {
            LOG.error("升级回滚失败", e);
            return AjaxResult.error(rootMessage(e));
        }
    }

    private static String rootMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message != null && !message.isBlank() ? message : error.toString();
    }
}
