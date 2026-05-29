package com.cat2bug.common.config;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

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

    /**
     * 读取磁盘 install 文件中 {@code cat2bug.install.completed}；文件不存在或解析失败时返回 false。
     */
    public static boolean isInstallCompletedOnDisk(String configPath)
    {
        Path path = resolveConfigPath(configPath);
        if (!Files.isRegularFile(path))
        {
            return false;
        }
        try (InputStream in = Files.newInputStream(path))
        {
            Object loaded = new Yaml().load(in);
            if (!(loaded instanceof Map<?, ?> root))
            {
                return false;
            }
            return readCompletedFlag(root);
        }
        catch (Exception ignored)
        {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private static boolean readCompletedFlag(Map<?, ?> root)
    {
        Object cat2bug = root.get("cat2bug");
        if (!(cat2bug instanceof Map<?, ?> installRoot))
        {
            return false;
        }
        Object install = installRoot.get("install");
        if (!(install instanceof Map<?, ?> installSection))
        {
            return false;
        }
        Object completed = installSection.get("completed");
        if (completed instanceof Boolean bool)
        {
            return bool;
        }
        if (completed != null)
        {
            return "true".equalsIgnoreCase(String.valueOf(completed).trim());
        }
        return false;
    }

    /**
     * 将 install 配置写入磁盘。
     */
    public static void writeInstallConfig(Path target, Map<String, Object> root) throws IOException
    {
        Files.createDirectories(target.getParent());
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        String content = new Yaml(options).dump(root);
        Files.writeString(target, content, StandardCharsets.UTF_8);
    }

    @SuppressWarnings("unchecked")
    public static void setInstallCompleted(Map<String, Object> root, boolean completed)
    {
        Map<String, Object> cat2bug = (Map<String, Object>) root.computeIfAbsent("cat2bug", key -> new LinkedHashMap<>());
        Map<String, Object> install = (Map<String, Object>) cat2bug.computeIfAbsent("install", key -> new LinkedHashMap<>());
        install.put("completed", completed);
    }
}
