package com.cat2bug.admin.resource;

import com.cat2bug.admin.setup.QuarkusInstallRuntimeActivator;
import com.cat2bug.admin.setup.SetupDatabaseTestService;
import com.cat2bug.admin.setup.SetupInstallService;
import com.cat2bug.admin.setup.SetupOllamaTestService;
import com.cat2bug.admin.setup.SetupPathTestService;
import com.cat2bug.admin.setup.SetupPreflightService;
import com.cat2bug.admin.setup.SetupRedisTestService;
import com.cat2bug.common.api.AjaxResult;
import com.cat2bug.common.domain.setup.SetupDatabaseTestRequest;
import com.cat2bug.common.domain.setup.SetupOllamaTestRequest;
import com.cat2bug.common.domain.setup.SetupPathTestRequest;
import com.cat2bug.common.domain.setup.SetupRedisTestRequest;
import com.cat2bug.common.domain.setup.SetupSubmitRequest;
import com.cat2bug.common.setup.SetupMessages;
import com.cat2bug.system.service.InstallStateService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.HashMap;
import java.util.Map;

@Path("/setup")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SetupResource {

    @Inject
    InstallStateService installStateService;

    @Inject
    SetupDatabaseTestService setupDatabaseTestService;

    @Inject
    SetupRedisTestService setupRedisTestService;

    @Inject
    SetupOllamaTestService setupOllamaTestService;

    @Inject
    SetupPathTestService setupPathTestService;

    @Inject
    SetupInstallService setupInstallService;

    @Inject
    QuarkusInstallRuntimeActivator installRuntimeActivator;

    @Inject
    SetupPreflightService setupPreflightService;

    @GET
    @Path("status")
    public AjaxResult status() {
        installRuntimeActivator.recoverStuckBootstrapIfNeeded();
        Map<String, Object> data = new HashMap<>();
        data.put("installed", installStateService.isInstalled());
        data.put("skipped", installStateService.isSkipped());
        data.put("restartRequired", installStateService.needsRestart());
        return AjaxResult.success(data);
    }

    @GET
    @Path("preflight")
    public AjaxResult preflight() {
        if (installStateService.isInstalled()) {
            return AjaxResult.error(SetupMessages.msg("setup.install.already"));
        }
        return AjaxResult.success(setupPreflightService.buildPreflight());
    }

    @POST
    @Path("test/database")
    public AjaxResult testDatabase(SetupDatabaseTestRequest request) {
        return wrapTestResult(setupDatabaseTestService.test(request));
    }

    @POST
    @Path("test/redis")
    public AjaxResult testRedis(SetupRedisTestRequest request) {
        return wrapTestResult(setupRedisTestService.test(request));
    }

    @POST
    @Path("test/ollama")
    public AjaxResult testOllama(SetupOllamaTestRequest request) {
        return wrapTestResult(setupOllamaTestService.test(request));
    }

    @POST
    @Path("test/path")
    public AjaxResult testPath(SetupPathTestRequest request) {
        return wrapTestResult(setupPathTestService.test(request));
    }

    @POST
    @Path("submit")
    public AjaxResult submit(SetupSubmitRequest request) throws Exception {
        if (installStateService.isInstalled()) {
            return AjaxResult.error(SetupMessages.msg("setup.install.already"));
        }
        Map<String, Object> data = setupInstallService.submit(request);
        return AjaxResult.success(String.valueOf(data.get("message")), data);
    }

    private AjaxResult wrapTestResult(Map<String, Object> testResult) {
        if (Boolean.TRUE.equals(testResult.get("success"))) {
            return AjaxResult.success(testResult);
        }
        return AjaxResult.error(String.valueOf(testResult.get("message")));
    }
}
