package com.cat2bug.common.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 安装配置文件路径解析（供启动早期 EnvironmentPostProcessor 与运行时共用）
 */
public final class InstallConfigSupport
{
    public static final String DEFAULT_CONFIG_PATH = "./config/install/application-install.yml";

    private InstallConfigSupport()
    {
    }

    public static Path resolveConfigPath(String configPath)
    {
        String path = configPath != null && !configPath.isBlank() ? configPath : DEFAULT_CONFIG_PATH;
        return Paths.get(path).toAbsolutePath().normalize();
    }

    public static boolean isConfigFilePresent(String configPath)
    {
        return Files.isRegularFile(resolveConfigPath(configPath));
    }

    public static boolean isInstallSkipped(String skipProperty)
    {
        return "true".equalsIgnoreCase(skipProperty)
                || "true".equalsIgnoreCase(System.getenv("CAT2BUG_INSTALL_SKIP"));
    }
}
