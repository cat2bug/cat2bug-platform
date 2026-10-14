package com.cat2bug.admin.setup;

import com.cat2bug.system.service.InstallStateService;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.Quarkus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 安装完成后延迟退出进程；dev 模式由 dev.sh 循环拉起，JAR 模式自拉起新进程。
 */
@ApplicationScoped
public class QuarkusInstallRestarter {

    private static final Logger log = Logger.getLogger(QuarkusInstallRestarter.class);
    private static final int RESTART_DELAY_MS = 2000;

    private final AtomicBoolean restartScheduled = new AtomicBoolean(false);

    @Inject
    InstallStateService installStateService;

    @org.eclipse.microprofile.config.inject.ConfigProperty(name = "cat2bug.install.allow-restart", defaultValue = "true")
    boolean allowRestart;

    public void scheduleRestartAfterSetup() {
        if (!allowRestart) {
            log.info("测试/禁用模式：跳过安装后自动退出");
            return;
        }
        if (!restartScheduled.compareAndSet(false, true)) {
            return;
        }
        Thread restartThread = new Thread(this::restartAfterDelay, "cat2bug-install-restart");
        restartThread.setDaemon(false);
        restartThread.start();
    }

    private void restartAfterDelay() {
        try {
            Thread.sleep(RESTART_DELAY_MS);
            boolean devMode = LaunchMode.current() == LaunchMode.DEVELOPMENT;
            boolean spawned = false;
            if (!devMode) {
                spawned = QuarkusApplicationSelfRestarter.spawn(installStateService.getConfigPath());
            }
            if (spawned) {
                log.infof("安装完成：新进程已拉起，当前进程 %d ms 后退出", RESTART_DELAY_MS);
            } else if (devMode) {
                log.infof("安装完成（dev 模式）：%d ms 后退出，由 dev.sh 重新拉起", RESTART_DELAY_MS);
            } else {
                log.infof("安装完成：%d ms 后退出进程（请使用 cat2bug-service.sh 或手动重启 JAR）", RESTART_DELAY_MS);
            }
            Quarkus.asyncExit(0);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warnf("安装重启线程被中断: %s", ex.getMessage());
        }
    }
}
