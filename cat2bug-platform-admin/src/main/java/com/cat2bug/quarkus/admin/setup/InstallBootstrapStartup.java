package com.cat2bug.quarkus.admin.setup;

import com.cat2bug.quarkus.system.service.InstallBootstrapState;
import com.cat2bug.quarkus.system.service.InstallStateService;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * 重启后若已加载 {@code quarkus-runtime.properties}（{@code cat2bug.bootstrap.mode=false}），
 * 标记运行期已激活，使 {@code /setup/status} 的 {@code restartRequired} 变为 false。
 */
@ApplicationScoped
public class InstallBootstrapStartup {

    private static final Logger log = Logger.getLogger(InstallBootstrapStartup.class);

    @Inject
    InstallStateService installStateService;

    @Inject
    InstallBootstrapState installBootstrapState;

    @Inject
    QuarkusInstallRuntimePropertiesWriter runtimePropertiesWriter;

    @ConfigProperty(name = "cat2bug.bootstrap.mode", defaultValue = "true")
    boolean bootstrapMode;

    void onStart(@Observes StartupEvent event) {
        if (!installStateService.isInstalled() || bootstrapMode) {
            return;
        }
        installBootstrapState.markRuntimeActivated();
        runtimePropertiesWriter.clearPendingRestartMarker();
        log.info("安装运行期配置已生效，系统已退出引导模式");
    }
}
