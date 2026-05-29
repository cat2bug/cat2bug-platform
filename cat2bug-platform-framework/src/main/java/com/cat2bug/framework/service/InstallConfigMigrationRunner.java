package com.cat2bug.framework.service;

import com.cat2bug.common.config.InstallConfigExporter;
import com.cat2bug.common.config.InstallConfigSupport;
import com.cat2bug.common.config.InstallProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Map;

/**
 * Legacy 实例自动迁移：磁盘无 install 文件但数据库已有 schema/管理员时，从 Environment 导出并写入 install。
 */
@Component
@Order(0)
public class InstallConfigMigrationRunner implements ApplicationRunner
{
    private static final Logger log = LoggerFactory.getLogger(InstallConfigMigrationRunner.class);

    @Autowired
    private InstallProperties installProperties;

    @Autowired
    private InstallService installService;

    @Autowired
    private UpgradeService upgradeService;

    @Autowired
    private Environment environment;

    @Override
    public void run(ApplicationArguments args) throws Exception
    {
        if (installService.isInstallSkipped())
        {
            return;
        }
        if (installProperties.isInstallConfigPresent() && installProperties.isInstallCompletedOnDisk())
        {
            return;
        }
        if (!installService.hasLegacyInstallation() && !installService.isSchemaPresent())
        {
            return;
        }

        if (!upgradeService.isUpgradeSkipped())
        {
            upgradeService.markPending();
            log.info("Legacy 实例已标记为待升级，请通过 /upgrade 向导完成配置与迁移");
            return;
        }

        Path target = installProperties.resolveConfigPath();
        Map<String, Object> root = InstallConfigExporter.exportForMigration(environment);
        InstallConfigSupport.setInstallCompleted(root, true);
        InstallConfigSupport.writeInstallConfig(target, root);
        log.info("Legacy 安装配置已自动迁移至 {}", target);
    }
}
