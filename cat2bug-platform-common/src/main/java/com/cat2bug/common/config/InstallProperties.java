package com.cat2bug.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

/**
 * 首次安装向导相关配置
 */
@Component
@ConfigurationProperties(prefix = "cat2bug.install")
public class InstallProperties
{
    /** 安装完成后写入的外部配置文件路径 */
    private String configPath = InstallConfigSupport.DEFAULT_CONFIG_PATH;

    /** 为 true 时跳过安装向导（也可通过环境变量 CAT2BUG_INSTALL_SKIP=true） */
    private boolean skip = false;

    public String getConfigPath()
    {
        return configPath;
    }

    public void setConfigPath(String configPath)
    {
        this.configPath = configPath;
    }

    public Path resolveConfigPath()
    {
        return InstallConfigSupport.resolveConfigPath(configPath);
    }

    /** 磁盘 install 配置文件是否存在（不参与安装完成判定） */
    public boolean isInstallConfigPresent()
    {
        return InstallConfigSupport.isConfigFilePresent(configPath);
    }

    /** 磁盘 install 中 {@code cat2bug.install.completed} 是否为 true */
    public boolean isInstallCompletedOnDisk()
    {
        return InstallConfigSupport.isInstallCompletedOnDisk(configPath);
    }

    public boolean isSkip()
    {
        return skip;
    }

    public void setSkip(boolean skip)
    {
        this.skip = skip;
    }

    public boolean isSkipFromEnv()
    {
        return "true".equalsIgnoreCase(System.getenv("CAT2BUG_INSTALL_SKIP"));
    }
}
