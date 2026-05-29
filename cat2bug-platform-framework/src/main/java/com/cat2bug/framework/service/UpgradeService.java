package com.cat2bug.framework.service;

import com.cat2bug.common.config.Cat2BugConfig;
import com.cat2bug.common.config.InstallConfigSupport;
import com.cat2bug.common.config.InstallProperties;
import com.cat2bug.common.config.UpgradeProperties;
import com.cat2bug.common.config.UpgradeSupport;
import com.cat2bug.common.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Legacy 升级向导状态与触发条件。
 */
@Service
@Order(1)
public class UpgradeService implements ApplicationRunner
{
    private static final Logger log = LoggerFactory.getLogger(UpgradeService.class);

    @Autowired
    private UpgradeProperties upgradeProperties;

    @Autowired
    private InstallProperties installProperties;

    @Autowired
    private InstallService installService;

    @Autowired
    private Environment environment;

    @Autowired
    private Cat2BugConfig cat2BugConfig;

    @Autowired(required = false)
    private UpgradeMigrationInspector migrationInspector;

    @Override
    public void run(ApplicationArguments args)
    {
        if (isUpgradeSkipped())
        {
            return;
        }
        syncFromDisk();
        if (UpgradeSupport.STATE_RESTART_REQUIRED.equals(readRawState()))
        {
            Map<String, Object> section = readSection();
            section.put("state", UpgradeSupport.STATE_COMPLETED);
            section.put("completedVersion", cat2BugConfig.getVersion());
            section.put("lastError", "");
            section.put("lastStep", "");
            writeSection(section);
            log.info("升级重启完成，状态已转为 completed");
        }
    }

    public boolean isUpgradeSkipped()
    {
        return upgradeProperties.isUpgradeSkipped();
    }

    public boolean isUpgradeActive()
    {
        return UpgradeSupport.isActiveState(normalizeState(readRawState()));
    }

    public boolean isUpgradeRequired()
    {
        if (isUpgradeSkipped())
        {
            return false;
        }
        if (isUpgradeActive())
        {
            return true;
        }
        if (installProperties.isInstallCompletedOnDisk())
        {
            return hasPendingSchemaUpgrade();
        }
        return hasLegacyUpgradeIndicators();
    }

    /** 无 install 完成标记时，仅依据已有业务数据/schema 判定 Legacy 升级，空库待执行 Flyway 脚本不算。 */
    public boolean hasLegacyUpgradeIndicators()
    {
        return installService.hasLegacyInstallation() || installService.isSchemaPresent();
    }

    public Map<String, Object> getStatus()
    {
        Map<String, Object> section = readSection();
        Map<String, Object> status = new HashMap<>(12);
        String state = normalizeState(stringValue(section.get("state")));
        status.put("upgradeRequired", isUpgradeRequired());
        status.put("state", state);
        status.put("targetVersion", StringUtils.isNotEmpty(stringValue(section.get("targetVersion")))
                ? stringValue(section.get("targetVersion")) : cat2BugConfig.getVersion());
        status.put("completedVersion", stringValue(section.get("completedVersion")));
        status.put("lastError", stringValue(section.get("lastError")));
        status.put("lastStep", stringValue(section.get("lastStep")));
        status.put("attemptCount", numberValue(section.get("attemptCount")));
        status.put("skipped", isUpgradeSkipped());
        status.put("restartRequired", UpgradeSupport.STATE_RESTART_REQUIRED.equals(readRawState()));
        status.put("pendingMigrations", listPendingMigrations());
        status.put("installed", installService.isInstalled());
        status.put("currentVersion", cat2BugConfig.getVersion());
        return status;
    }

    public void markPending()
    {
        if (isUpgradeSkipped())
        {
            return;
        }
        Map<String, Object> section = readSection();
        if (UpgradeSupport.isActiveState(stringValue(section.get("state"))))
        {
            return;
        }
        section.put("state", UpgradeSupport.STATE_PENDING);
        section.put("targetVersion", cat2BugConfig.getVersion());
        section.put("attemptCount", 0);
        section.put("lastError", "");
        section.put("lastStep", "");
        section.put("startedAt", Instant.now().toString());
        writeSection(section);
        log.info("Legacy 实例已标记为待升级");
    }

    public void markRunning()
    {
        Map<String, Object> section = readSection();
        section.put("state", UpgradeSupport.STATE_RUNNING);
        section.put("attemptCount", numberValue(section.get("attemptCount")) + 1);
        writeSection(section);
    }

    public void markFailed(String lastStep, String error)
    {
        Map<String, Object> section = readSection();
        section.put("state", UpgradeSupport.STATE_FAILED);
        section.put("lastStep", lastStep);
        section.put("lastError", error != null ? error : "");
        writeSection(section);
    }

    public void markRestartRequired()
    {
        Map<String, Object> section = readSection();
        section.put("state", UpgradeSupport.STATE_RESTART_REQUIRED);
        section.put("lastStep", "");
        section.put("lastError", "");
        writeSection(section);
    }

    public void updateLastStep(String lastStep)
    {
        Map<String, Object> section = readSection();
        section.put("lastStep", lastStep);
        writeSection(section);
    }

    public String getLastStep()
    {
        return stringValue(readSection().get("lastStep"));
    }

    public String resolveState()
    {
        syncFromDisk();
        return normalizeState(readRawState());
    }

    private String readRawState()
    {
        return stringValue(readSection().get("state"));
    }

    /**
     * 磁盘缺省 {@code completed} 但升级尚未真正完成时，对外暴露为 {@code pending}，避免前端误判为成功。
     */
    private String normalizeState(String rawState)
    {
        if (!UpgradeSupport.STATE_COMPLETED.equals(rawState))
        {
            return rawState;
        }
        if (isUpgradeSkipped() || installProperties.isInstallCompletedOnDisk())
        {
            return rawState;
        }
        if (installService.hasLegacyInstallation() || installService.isSchemaPresent())
        {
            return UpgradeSupport.STATE_PENDING;
        }
        return rawState;
    }

    public boolean hasPendingSchemaUpgrade()
    {
        return !listPendingMigrations().isEmpty();
    }

    public List<String> listPendingMigrations()
    {
        if (migrationInspector == null)
        {
            return List.of();
        }
        try
        {
            return migrationInspector.listPendingMigrations(
                    environment.getProperty("spring.database-type", "h2"));
        }
        catch (Exception e)
        {
            return List.of();
        }
    }

    private Map<String, Object> readSection()
    {
        Map<String, Object> fromDisk = InstallConfigSupport.readUpgradeState(
                installProperties.getConfigPath(),
                InstallConfigSupport.resolveUpgradeStatePath(
                        installProperties.getConfigPath(),
                        upgradeProperties.getStatePath()).toString());
        if (!fromDisk.isEmpty())
        {
            return new LinkedHashMap<>(fromDisk);
        }
        Map<String, Object> section = new LinkedHashMap<>();
        section.put("state", upgradeProperties.getState());
        section.put("lastError", upgradeProperties.getLastError());
        section.put("lastStep", upgradeProperties.getLastStep());
        section.put("attemptCount", upgradeProperties.getAttemptCount());
        section.put("targetVersion", upgradeProperties.getTargetVersion());
        section.put("completedVersion", upgradeProperties.getCompletedVersion());
        return section;
    }

    private void writeSection(Map<String, Object> section)
    {
        try
        {
            InstallConfigSupport.writeUpgradeState(
                    installProperties.getConfigPath(),
                    InstallConfigSupport.resolveUpgradeStatePath(
                            installProperties.getConfigPath(),
                            upgradeProperties.getStatePath()).toString(),
                    section);
            upgradeProperties.setState(stringValue(section.get("state")));
            upgradeProperties.setLastError(stringValue(section.get("lastError")));
            upgradeProperties.setLastStep(stringValue(section.get("lastStep")));
            upgradeProperties.setAttemptCount(numberValue(section.get("attemptCount")));
            upgradeProperties.setTargetVersion(stringValue(section.get("targetVersion")));
            upgradeProperties.setCompletedVersion(stringValue(section.get("completedVersion")));
        }
        catch (Exception ex)
        {
            throw new IllegalStateException("无法写入升级状态: " + ex.getMessage(), ex);
        }
    }

    private void syncFromDisk()
    {
        Map<String, Object> section = InstallConfigSupport.readUpgradeState(
                installProperties.getConfigPath(),
                InstallConfigSupport.resolveUpgradeStatePath(
                        installProperties.getConfigPath(),
                        upgradeProperties.getStatePath()).toString());
        if (section.isEmpty())
        {
            return;
        }
        upgradeProperties.setState(stringValue(section.get("state")));
        upgradeProperties.setLastError(stringValue(section.get("lastError")));
        upgradeProperties.setLastStep(stringValue(section.get("lastStep")));
        upgradeProperties.setAttemptCount(numberValue(section.get("attemptCount")));
        upgradeProperties.setTargetVersion(stringValue(section.get("targetVersion")));
        upgradeProperties.setCompletedVersion(stringValue(section.get("completedVersion")));
    }

    private static String stringValue(Object value)
    {
        return value == null ? "" : String.valueOf(value);
    }

    private static int numberValue(Object value)
    {
        if (value instanceof Number number)
        {
            return number.intValue();
        }
        if (value != null)
        {
            try
            {
                return Integer.parseInt(String.valueOf(value));
            }
            catch (NumberFormatException ignored)
            {
                return 0;
            }
        }
        return 0;
    }
}
