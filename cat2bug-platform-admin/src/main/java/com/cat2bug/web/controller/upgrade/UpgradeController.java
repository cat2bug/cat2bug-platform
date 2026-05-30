package com.cat2bug.web.controller.upgrade;

import com.cat2bug.common.core.controller.BaseController;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.framework.service.UpgradeService;
import com.cat2bug.web.domain.setup.SetupSubmitRequest;
import com.cat2bug.web.service.upgrade.UpgradeInstallService;
import com.cat2bug.web.service.upgrade.UpgradePreflightService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Legacy 升级向导 API
 */
@RestController
@RequestMapping("/upgrade")
public class UpgradeController extends BaseController
{
    @Autowired
    private UpgradeService upgradeService;

    @Autowired
    private UpgradePreflightService upgradePreflightService;

    @Autowired
    private UpgradeInstallService upgradeInstallService;

    @GetMapping("/status")
    public AjaxResult status()
    {
        return success(upgradeService.getStatus());
    }

    @GetMapping("/preflight")
    public AjaxResult preflight()
    {
        return success(upgradePreflightService.buildPreflight());
    }

    @PostMapping("/submit")
    public AjaxResult submit(@RequestBody SetupSubmitRequest request)
    {
        Map<String, Object> data = upgradeInstallService.submit(request, false);
        return AjaxResult.success(String.valueOf(data.get("message")), data);
    }

    @PostMapping("/retry")
    public AjaxResult retry(@RequestBody SetupSubmitRequest request)
    {
        Map<String, Object> data = upgradeInstallService.submit(request, true);
        return AjaxResult.success(String.valueOf(data.get("message")), data);
    }

    @PostMapping("/rollback")
    public AjaxResult rollback(@RequestBody SetupSubmitRequest request)
    {
        Map<String, Object> data = upgradeInstallService.rollback(request);
        return AjaxResult.success(String.valueOf(data.get("message")), data);
    }
}
