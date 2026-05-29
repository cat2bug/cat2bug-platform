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

/**
 * 首次安装前启动引导：在磁盘 install 不存在时从 classpath H2 模板注入内存配置，避免连接尚未配置的外部数据库。
 */
public final class InstallStartupSupport
{
    private static final Logger log = LoggerFactory.getLogger(InstallStartupSupport.class);

    public static final String BOOTSTRAP_MODE_PROPERTY = "cat2bug.install.bootstrap-mode";

    private InstallStartupSupport()
    {
    }

    /**
     * 在 {@link SpringApplication#run} 之前调用，处理命令行参数与 JVM 级属性。
     */
    public static String[] prepare(String[] args)
    {
        BootstrapDecision decision = evaluate(null, null);
        if (!decision.bootstrap())
        {
            return args;
        }
        String[] filtered = filterDatabaseArgs(args);
        applyBootstrapProperties(decision.configPath());
        log.info("未找到安装配置文件 {}，首次安装引导模式：暂使用 H2 classpath 模板启动，完成向导并重启后生效所选数据库",
                decision.configPath());
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
        Path path = InstallConfigSupport.resolveConfigPath(resolvedPath);
        if (InstallConfigSupport.isConfigFilePresent(resolvedPath))
        {
            return BootstrapDecision.disabled(path);
        }
        if (InstallConfigSupport.isInstallSkipped(skip))
        {
            return BootstrapDecision.disabled(path);
        }
        return BootstrapDecision.enabled(path);
    }

    /**
     * 引导期注入 Environment 的扁平属性（H2 classpath 模板 + bootstrap-mode）。
     */
    public static Map<String, Object> bootstrapProperties()
    {
        Map<String, Object> bootstrap = new LinkedHashMap<>(
                InstallTemplateLoader.flattenToProperties(InstallTemplateLoader.loadH2Template()));
        bootstrap.put(BOOTSTRAP_MODE_PROPERTY, "true");
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
