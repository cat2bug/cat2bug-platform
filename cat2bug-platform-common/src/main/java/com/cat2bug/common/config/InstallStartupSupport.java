package com.cat2bug.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * 首次安装前启动引导：在磁盘 install 不存在时从 classpath H2 模板注入内存配置，避免连接尚未配置的外部数据库。
 */
public final class InstallStartupSupport
{
    private static final Logger log = LoggerFactory.getLogger(InstallStartupSupport.class);

    private static volatile boolean bootstrapNoticeLogged;

    public static final String BOOTSTRAP_MODE_PROPERTY = "cat2bug.install.bootstrap-mode";

    /** 引导期专用 H2 文件，与用户选择的 {@code cat2bug_platform} 等库名隔离，避免误判 existing。 */
    public static final String BOOTSTRAP_H2_JDBC_URL =
            "jdbc:h2:file:./data/.cat2bug_bootstrap;MODE=MySQL;DATABASE_TO_LOWER=TRUE;";

    private static final String DRUID_MASTER_URL_PROPERTY = "spring.datasource.druid.master.url";

    private InstallStartupSupport()
    {
    }

    /**
     * 在 {@link SpringApplication#run} 之前调用，处理命令行参数与 JVM 级属性。
     */
    public static String[] prepare(String[] args)
    {
        pinResolvedInstallConfigPathIfPresent();
        BootstrapDecision decision = evaluate(null, null);
        if (!decision.bootstrap())
        {
            return args;
        }
        String[] filtered = filterDatabaseArgs(args);
        applyBootstrapProperties(decision.configPath());
        logBootstrapNotice(decision.configPath());
        return filtered;
    }

    public static void applyDefaultProperties(SpringApplication application)
    {
        BootstrapDecision decision = evaluate(null, null);
        if (!decision.bootstrap())
        {
            return;
        }
        application.setDefaultProperties(bootstrapProperties());
    }

    public static BootstrapDecision evaluate(String configPath, String skipProperty)
    {
        String resolvedPath = configPath != null ? configPath : resolveConfigPathProperty();
        String skip = skipProperty != null ? skipProperty : System.getProperty("cat2bug.install.skip", "false");
        Path path = InstallConfigSupport.resolveWorkingDirectoryConfigPath(resolvedPath);
        if (InstallConfigSupport.isConfigFilePresent(resolvedPath))
        {
            return BootstrapDecision.disabled(InstallConfigSupport.resolveConfigPath(resolvedPath));
        }
        if (InstallConfigSupport.isInstallSkipped(skip))
        {
            return BootstrapDecision.disabled(path);
        }
        return BootstrapDecision.enabled(path);
    }

    /**
     * 将已定位的 install 配置绝对路径写入系统属性，供 Spring {@code config.import} 与 {@link InstallProperties} 使用。
     */
    static void pinResolvedInstallConfigPathIfPresent()
    {
        String configPathProperty = resolveConfigPathProperty();
        Optional<Path> located = InstallConfigSupport.locateExistingConfigFile(configPathProperty);
        if (located.isEmpty())
        {
            return;
        }
        Path resolved = located.get();
        Path workingDirectoryCandidate = InstallConfigSupport.resolveWorkingDirectoryConfigPath(configPathProperty);
        System.setProperty("cat2bug.install.config-path", resolved.toString());
        System.setProperty("spring.config.import", "optional:file:" + resolved);
        if (!resolved.equals(workingDirectoryCandidate))
        {
            log.info("工作目录未找到安装配置，已从 JAR 同目录加载: {}", resolved);
        }
    }

    /**
     * 引导期注入 Environment 的扁平属性（H2 classpath 模板 + bootstrap-mode）。
     */
    public static Map<String, Object> bootstrapProperties()
    {
        Map<String, Object> bootstrap = new LinkedHashMap<>(
                InstallTemplateLoader.flattenToProperties(InstallTemplateLoader.loadH2Template()));
        bootstrap.put(BOOTSTRAP_MODE_PROPERTY, "true");
        bootstrap.put(DRUID_MASTER_URL_PROPERTY, BOOTSTRAP_H2_JDBC_URL);
        return bootstrap;
    }

    private static String resolveConfigPathProperty()
    {
        String envPath = System.getenv("CAT2BUG_INSTALL_CONFIG_PATH");
        if (envPath != null && !envPath.isBlank())
        {
            return envPath;
        }
        return System.getProperty("cat2bug.install.config-path", InstallConfigSupport.DEFAULT_CONFIG_PATH);
    }

    private static void applyBootstrapProperties(Path configPath)
    {
        bootstrapProperties().forEach((key, value) ->
        {
            if (value != null)
            {
                System.setProperty(key, String.valueOf(value));
            }
        });
    }

    static String[] filterDatabaseArgs(String[] args)
    {
        if (args == null || args.length == 0)
        {
            return args;
        }
        List<String> filtered = new ArrayList<>(args.length);
        for (int i = 0; i < args.length; i++)
        {
            String arg = args[i];
            if (shouldSkipDatabaseArg(arg))
            {
                if (isStandaloneOption(arg) && i + 1 < args.length)
                {
                    i++;
                }
                continue;
            }
            filtered.add(arg);
        }
        return filtered.toArray(new String[0]);
    }

    /** DevTools 重启会再次进入引导逻辑，安装提示只输出一次。 */
    public static void logBootstrapNotice(Path configPath)
    {
        if (bootstrapNoticeLogged)
        {
            return;
        }
        bootstrapNoticeLogged = true;
        log.info("未找到安装配置文件 {}，首次安装引导模式：暂使用 H2 classpath 模板启动，完成向导并重启后生效所选数据库",
                configPath);
    }

    private static boolean shouldSkipDatabaseArg(String arg)
    {
        if (arg == null)
        {
            return false;
        }
        String normalized = arg.toLowerCase(Locale.ROOT);
        return normalized.startsWith("--spring.database-type=")
                || normalized.equals("--spring.database-type")
                || normalized.startsWith("--spring.profiles.active=")
                || normalized.equals("--spring.profiles.active")
                || normalized.startsWith("-dspring.database-type=")
                || normalized.startsWith("-dspring.profiles.active=");
    }

    private static boolean isStandaloneOption(String arg)
    {
        return "--spring.database-type".equalsIgnoreCase(arg)
                || "--spring.profiles.active".equalsIgnoreCase(arg);
    }

    public record BootstrapDecision(boolean bootstrap, Path configPath)
    {
        static BootstrapDecision enabled(Path configPath)
        {
            return new BootstrapDecision(true, configPath);
        }

        static BootstrapDecision disabled(Path configPath)
        {
            return new BootstrapDecision(false, configPath);
        }
    }
}
