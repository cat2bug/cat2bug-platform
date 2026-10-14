package com.cat2bug.admin.setup;

import com.cat2bug.common.config.InstallConfigSupport;
import com.cat2bug.common.domain.setup.SetupSubmitRequest;
import com.cat2bug.common.util.StringUtils;
import com.cat2bug.system.service.InstallBootstrapState;
import com.cat2bug.system.service.InstallStateService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * 安装完成后写入 Quarkus 运行期属性并触发进程重启（对齐 Spring InstallRuntimeActivator + Restarter）。
 */
@ApplicationScoped
public class QuarkusInstallRuntimeActivator {

    private static final Logger log = Logger.getLogger(QuarkusInstallRuntimeActivator.class);

    @Inject
    InstallStateService installStateService;

    @Inject
    InstallBootstrapState installBootstrapState;

    @Inject
    QuarkusInstallRuntimePropertiesWriter runtimePropertiesWriter;

    @Inject
    QuarkusInstallRestarter installRestarter;

    /**
     * @return true 表示已安排重启且前端应继续等待；false 表示无需重启（跳过安装等）
     */
    public boolean activateAfterSetup(SetupSubmitRequest request) {
        String cacheType = StringUtils.isNotEmpty(request.getCacheType())
                ? request.getCacheType().toLowerCase() : "local";
        try {
            runtimePropertiesWriter.writeFromSubmit(request);
            log.infof("已写入 Quarkus 运行期配置: %s", runtimePropertiesWriter.runtimePropertiesPath());
        } catch (Exception e) {
            log.warnf("写入 Quarkus 运行期配置失败: %s", e.getMessage());
            return true;
        }
        if ("redis".equals(cacheType)) {
            log.info("安装向导：已选择 Redis 缓存，将重启进程以加载缓存配置");
        }
        installRestarter.scheduleRestartAfterSetup();
        return true;
    }

    /**
     * 已安装但仍在引导模式（例如用户停留在等待重启页）：补写运行期配置。
     * 不在 dev 模式反复触发进程退出（由用户手动重启 dev.sh）。
     */
    public void recoverStuckBootstrapIfNeeded() {
        if (installBootstrapState.isRuntimeActivated()) {
            return;
        }
        if (!installStateService.isInstalled() || !installStateService.needsRestart()) {
            return;
        }
        try {
            if (!InstallConfigSupport.isConfigFilePresent(installStateService.getConfigPath())) {
                return;
            }
            runtimePropertiesWriter.writeFromInstallFile();
            log.info("检测到安装已完成但仍处于引导模式，已补写 Quarkus 运行期配置（请重启 dev.sh）");
        } catch (Exception e) {
            log.warnf("引导模式恢复失败: %s", e.getMessage());
        }
    }
}
