package com.cat2bug.web.controller.setup;

import com.cat2bug.common.core.controller.BaseController;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.framework.service.InstallService;
import com.cat2bug.web.domain.setup.SetupDatabaseTestRequest;
import com.cat2bug.web.domain.setup.SetupOllamaTestRequest;
import com.cat2bug.web.domain.setup.SetupPathTestRequest;
import com.cat2bug.web.domain.setup.SetupRedisTestRequest;
import com.cat2bug.web.domain.setup.SetupSubmitRequest;
import com.cat2bug.web.service.setup.SetupDatabaseTestService;
import com.cat2bug.web.service.setup.SetupMessages;
import com.cat2bug.web.service.setup.SetupInstallService;
import com.cat2bug.web.service.setup.SetupOllamaTestService;
import com.cat2bug.web.service.setup.SetupPathTestService;
import com.cat2bug.web.service.setup.SetupRedisTestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 首次安装向导 API
 */
@RestController
@RequestMapping("/setup")
public class SetupController extends BaseController
{
    @Autowired
    private InstallService installService;

    @Autowired
    private SetupDatabaseTestService setupDatabaseTestService;

    @Autowired
    private SetupRedisTestService setupRedisTestService;

    @Autowired
    private SetupOllamaTestService setupOllamaTestService;

    @Autowired
    private SetupPathTestService setupPathTestService;

    @Autowired
    private SetupInstallService setupInstallService;

    @GetMapping("/status")
    public AjaxResult status()
    {
        Map<String, Object> data = new HashMap<>(3);
        data.put("installed", installService.isInstalled());
        data.put("skipped", installService.isInstallSkipped());
        data.put("restartRequired", installService.needsRestart());
        return success(data);
    }

    @PostMapping("/test/database")
    public AjaxResult testDatabase(@RequestBody SetupDatabaseTestRequest request)
    {
        return wrapTestResult(setupDatabaseTestService.test(request));
    }

    @PostMapping("/test/redis")
    public AjaxResult testRedis(@RequestBody SetupRedisTestRequest request)
    {
        return wrapTestResult(setupRedisTestService.test(request));
    }

    @PostMapping("/test/ollama")
    public AjaxResult testOllama(@RequestBody SetupOllamaTestRequest request)
    {
        return wrapTestResult(setupOllamaTestService.test(request));
    }

    @PostMapping("/test/path")
    public AjaxResult testPath(@RequestBody SetupPathTestRequest request)
    {
        return wrapTestResult(setupPathTestService.test(request));
    }

    @PostMapping("/submit")
    public AjaxResult submit(@RequestBody SetupSubmitRequest request) throws Exception
    {
        if (installService.isInstalled())
        {
            return error(SetupMessages.msg("setup.install.already"));
        }
        Map<String, Object> data = setupInstallService.submit(request);
        return AjaxResult.success(data.get("message").toString(), data);
    }

    private AjaxResult wrapTestResult(Map<String, Object> testResult)
    {
        if (Boolean.TRUE.equals(testResult.get("success")))
        {
            return success(testResult);
        }
        return error(testResult.get("message").toString());
    }
}
