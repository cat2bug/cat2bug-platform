package com.cat2bug.web.service.setup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;

/**
 * 安装完成后延迟退出 JVM，由 Docker / systemd / 开发工具重新拉起进程。
 */
@Service
public class InstallApplicationRestarter
{
    private static final Logger log = LoggerFactory.getLogger(InstallApplicationRestarter.class);

    private static final int RESTART_DELAY_MS = 2000;

    @Autowired
    private ConfigurableApplicationContext applicationContext;

    public void scheduleRestartAfterSetup()
    {
        Thread restartThread = new Thread(this::restartAfterDelay, "cat2bug-install-restart");
        restartThread.setDaemon(false);
        restartThread.start();
    }

    private void restartAfterDelay()
    {
        try
        {
            Thread.sleep(RESTART_DELAY_MS);
            log.info("安装向导完成：{} ms 后退出进程以加载新配置（需外部进程管理器或手动重新启动）", RESTART_DELAY_MS);
            int exitCode = SpringApplication.exit(applicationContext, () -> 0);
            System.exit(exitCode);
        }
        catch (InterruptedException ex)
        {
            Thread.currentThread().interrupt();
            log.warn("安装重启线程被中断: {}", ex.getMessage());
        }
    }
}
