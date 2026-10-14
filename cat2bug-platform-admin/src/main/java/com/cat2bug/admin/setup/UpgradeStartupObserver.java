package com.cat2bug.admin.setup;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * 启动时同步升级状态（对齐 Spring {@code UpgradeService#run}）。
 */
@ApplicationScoped
public class UpgradeStartupObserver {

    @Inject
    UpgradeService upgradeService;

    void onStart(@Observes StartupEvent event) {
        upgradeService.onStartup();
    }
}
