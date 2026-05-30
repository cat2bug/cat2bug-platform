package com.cat2bug.web.service.setup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.ApplicationArguments;
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

    @Autowired
    private ApplicationArguments applicationArguments;

    public void scheduleRestartAfterSetup()
    {
        String[] args = applicationArguments.getSourceArgs();
        Thread restartThread = new Thread(() -> restartAfterDelay(args), "cat2bug-install-restart");
        restartThread.setDaemon(false);
        restartThread.start();
    }

    private void restartAfterDelay(String[] applicationArgs)
    {
        try
        {
            Thread.sleep(RESTART_DELAY_MS);
            boolean spawned = ApplicationSelfRestarter.spawn(applicationArgs);
            if (spawned)
            {
                log.info("安装/升级完成：新进程已拉起，当前进程 {} ms 后退出", RESTART_DELAY_MS);
            }
            else
            {
                log.info("安装/升级完成：{} ms 后退出进程（Docker/systemd/脚本将负责拉起）", RESTART_DELAY_MS);
            }
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
