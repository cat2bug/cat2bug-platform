package com.cat2bug.framework.service;

import com.cat2bug.common.config.InstallProperties;
import com.cat2bug.common.config.InstallStartupSupport;
import com.cat2bug.common.config.UpgradeSupport;
import com.cat2bug.common.utils.FlywaySchemaSupport;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.system.domain.SysConfig;
import com.cat2bug.system.service.ISysConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 安装状态检测与标记
 */
@Service
public class InstallService implements ApplicationRunner
{
    private static final Logger log = LoggerFactory.getLogger(InstallService.class);

    public static final String INSTALL_COMPLETED_KEY = "cat2bug.install.completed";

    public static final Long LEGACY_ADMIN_USER_ID = 1L;

    /** @deprecated 仅兼容旧文档；legacy 检测以 {@link #LEGACY_ADMIN_USER_ID} 为准 */
    @Deprecated
    public static final String LEGACY_ADMIN_USERNAME = "admin";

    @Autowired
    private InstallProperties installProperties;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Lazy
    @Autowired
    private ISysConfigService configService;

    @Lazy
    @Autowired
    private UpgradeService upgradeService;

    @Autowired
    private Environment environment;

    @Override
    public void run(ApplicationArguments args)
    {
        // 安装状态以 application-install.yml 为准；sys_config 中的完成标记仅作审计，见 markCompleted。
    }

    public boolean isInstallSkipped()
    {
        return installProperties.isSkip() || installProperties.isSkipFromEnv();
    }

    /**
     * 已安装：跳过标志，或磁盘 install 中 {@code cat2bug.install.completed == true}。
     * 不以 install 文件是否存在、也不以 sys_config 中的 {@link #INSTALL_COMPLETED_KEY} 判定。
     */
    public boolean isInstalled()
    {
        if (isInstallSkipped())
        {
            return true;
        }
        return installProperties.isInstallCompletedOnDisk();
    }

    /**
     * 安装文件已写入但当前 JVM 仍处于引导模式（未重启加载 install 配置）。
     * 此状态下业务 API 仍使用引导期 H2，MySQL 等配置尚未生效。
     */
    public boolean needsRestart()
    {
        if (isInstallSkipped())
        {
            return false;
        }
        if (!installProperties.isInstallCompletedOnDisk())
        {
            return false;
        }
        if (upgradeService != null
                && UpgradeSupport.STATE_RESTART_REQUIRED.equals(upgradeService.resolveState()))
        {
            // 升级向导已接管「重启后生效」流程，避免 /setup/status.restartRequired 误导向 /setup
            return false;
        }
        return "true".equalsIgnoreCase(
                environment.getProperty(InstallStartupSupport.BOOTSTRAP_MODE_PROPERTY, "false"));
    }

    /**
     * 安装向导完成时写入 sys_config 的标记（审计/兼容用，不参与 {@link #isInstalled()}）。
     */
    public boolean isInstallCompletedInDatabase()
    {
        try
        {
            String value = jdbcTemplate.queryForObject(
                    "SELECT config_value FROM sys_config WHERE config_key = ? LIMIT 1",
                    String.class,
                    INSTALL_COMPLETED_KEY);
            return StringUtils.isNotEmpty(value) && "true".equalsIgnoreCase(value.trim());
        }
        catch (Exception e)
        {
            return false;
        }
    }

    /**
     * Legacy：SQL 种子首条管理员（user_id=1）仍存在，与用户名是否仍为 admin 无关。
     * 仅用于兼容检测，不参与 {@link #isInstalled()}。
     */
    public boolean hasLegacyInstallation()
    {
        try
        {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM sys_user WHERE user_id = ? AND del_flag = '0'",
                    Integer.class,
                    LEGACY_ADMIN_USER_ID);
            return count != null && count > 0;
        }
        catch (Exception e)
        {
            return false;
        }
    }

    /** @deprecated 使用 {@link #hasLegacyInstallation()} */
    @Deprecated
    public boolean hasLegacyAdminUser()
    {
        try
        {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM sys_user WHERE user_name = ? AND del_flag = '0'",
                    Integer.class,
                    LEGACY_ADMIN_USERNAME);
            return count != null && count > 0;
        }
        catch (Exception e)
        {
            return false;
        }
    }

    public boolean isSchemaPresent()
    {
        return FlywaySchemaSupport.hasSuccessfulMigration(jdbcTemplate);
    }

    public void markCompleted()
    {
        try
        {
            SysConfig query = new SysConfig();
            query.setConfigKey(INSTALL_COMPLETED_KEY);
            List<SysConfig> list = configService.selectConfigList(query);
            if (!list.isEmpty())
            {
                SysConfig config = list.get(0);
                config.setConfigValue("true");
                configService.updateConfig(config);
            }
            else
            {
                SysConfig config = new SysConfig();
                config.setConfigName("安装完成");
                config.setConfigKey(INSTALL_COMPLETED_KEY);
                config.setConfigValue("true");
                config.setConfigType("Y");
                config.setCreateBy("system");
                config.setRemark("首次安装向导完成标记");
                configService.insertConfig(config);
            }
        }
        catch (Exception e)
        {
            log.warn("无法写入安装完成标记: {}", e.getMessage());
        }
    }
}
