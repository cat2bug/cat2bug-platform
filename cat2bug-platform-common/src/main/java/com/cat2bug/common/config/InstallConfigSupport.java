package com.cat2bug.common.config;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 安装配置文件路径解析（供启动早期 EnvironmentPostProcessor 与运行时共用）
 */
public final class InstallConfigSupport
{
    public static final String DEFAULT_CONFIG_PATH = "./config/install/application-install.yml";

    public static final String DEFAULT_UPGRADE_STATE_PATH = "./config/install/application-upgrade-state.yml";

    public static final String UPGRADE_STATE_PENDING = "pending";

    public static final String UPGRADE_STATE_RUNNING = "running";

    public static final String UPGRADE_STATE_FAILED = "failed";

    public static final String UPGRADE_STATE_RESTART_REQUIRED = "restart_required";

    public static final String UPGRADE_STATE_COMPLETED = "completed";

    public static final String UPGRADE_LAST_STEP_BACKUP = "backup";

    public static final String UPGRADE_LAST_STEP_CONFIG = "config";

    public static final String UPGRADE_LAST_STEP_MIGRATION = "migration";

    /** @deprecated 使用 {@link #UPGRADE_LAST_STEP_CONFIG} */
    public static final String UPGRADE_STEP_CONFIG = UPGRADE_LAST_STEP_CONFIG;

    /** @deprecated 使用 {@link #UPGRADE_LAST_STEP_MIGRATION} */
    public static final String UPGRADE_STEP_MIGRATION = UPGRADE_LAST_STEP_MIGRATION;

    private InstallConfigSupport()
    {
    }

    /**
     * 解析 install 配置路径：若磁盘上已存在则返回实际位置（含 JAR 同目录回退），否则返回工作目录下的目标路径（供写入）。
     */
    public static Path resolveConfigPath(String configPath)
    {
        return locateExistingConfigFile(configPath).orElseGet(() -> resolveWorkingDirectoryConfigPath(configPath));
    }

    public static Path resolveWorkingDirectoryConfigPath(String configPath)
    {
        String path = normalizeConfigPath(configPath);
        return Paths.get(path).toAbsolutePath().normalize();
    }

    public static boolean isConfigFilePresent(String configPath)
    {
        return locateExistingConfigFile(configPath).isPresent();
    }

    /**
     * 查找已存在的 install 配置文件：优先工作目录，其次 JAR（或 classes）所在目录下的相对路径。
     */
    public static Optional<Path> locateExistingConfigFile(String configPath)
    {
        return locateExistingConfigFile(configPath, resolveJarDirectory());
    }

    static Optional<Path> locateExistingConfigFile(String configPath, Path jarDirectory)
    {
        String path = normalizeConfigPath(configPath);
        Path workingDirectoryCandidate = Paths.get(path).toAbsolutePath().normalize();
        if (Files.isRegularFile(workingDirectoryCandidate))
        {
            return Optional.of(workingDirectoryCandidate);
        }
        if (Paths.get(path).isAbsolute() || jarDirectory == null)
        {
            return Optional.empty();
        }
        Path jarDirectoryCandidate = jarDirectory.resolve(path).toAbsolutePath().normalize();
        if (Files.isRegularFile(jarDirectoryCandidate))
        {
            return Optional.of(jarDirectoryCandidate);
        }
        return Optional.empty();
    }

    /**
     * 可执行 JAR 或 IDE {@code target/classes} 的父目录（即 JAR 同目录）。
     */
    public static Path resolveJarDirectory()
    {
        Path fromCodeSource = resolveJarDirectoryFromCodeSource();
        if (fromCodeSource != null)
        {
            return fromCodeSource;
        }
        return resolveJarDirectoryFromClassPath();
    }

    private static Path resolveJarDirectoryFromCodeSource()
    {
        try
        {
            var protectionDomain = InstallConfigSupport.class.getProtectionDomain();
            if (protectionDomain == null || protectionDomain.getCodeSource() == null)
            {
                return null;
            }
            Path codePath = toLocalPath(protectionDomain.getCodeSource().getLocation().toURI());
            if (codePath == null)
            {
                return null;
            }
            codePath = codePath.toAbsolutePath().normalize();
            if (Files.isRegularFile(codePath))
            {
                Path parent = codePath.getParent();
                return parent != null ? parent.toAbsolutePath().normalize() : null;
            }
            if (Files.isDirectory(codePath))
            {
                Path parent = codePath.getParent();
                return parent != null ? parent.toAbsolutePath().normalize() : null;
            }
        }
        catch (URISyntaxException | SecurityException ignored)
        {
            return null;
        }
        return null;
    }

    /**
     * {@code java -jar app.jar} 时 {@code java.class.path} 即为可执行 JAR 路径。
     */
    private static Path resolveJarDirectoryFromClassPath()
    {
        String classPath = System.getProperty("java.class.path", "");
        if (classPath.isBlank() || classPath.contains(java.io.File.pathSeparator))
        {
            return null;
        }
        Path jarFile = Paths.get(classPath).toAbsolutePath().normalize();
        if (!Files.isRegularFile(jarFile) || !jarFile.getFileName().toString().endsWith(".jar"))
        {
            return null;
        }
        Path parent = jarFile.getParent();
        return parent != null ? parent.toAbsolutePath().normalize() : null;
    }

    static Path toLocalPath(URI location) throws URISyntaxException
    {
        if (location == null)
        {
            return null;
        }
        if ("file".equalsIgnoreCase(location.getScheme()))
        {
            return Paths.get(location);
        }
        if ("jar".equalsIgnoreCase(location.getScheme()))
        {
            String spec = location.getRawSchemeSpecificPart();
            int separator = spec.indexOf('!');
            String fileUri = separator >= 0 ? spec.substring(0, separator) : spec;
            return Paths.get(URI.create(fileUri));
        }
        return null;
    }

    private static String normalizeConfigPath(String configPath)
    {
        return configPath != null && !configPath.isBlank() ? configPath : DEFAULT_CONFIG_PATH;
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

    /**
     * 读取升级状态；优先 install 文件中的 cat2bug.upgrade，否则读侧车 state 文件。
     */
    public static Map<String, Object> readUpgradeState(String configPath)
    {
        return readUpgradeState(configPath, resolveUpgradeStatePath(configPath, null).toString());
    }

    /**
     * 读取升级状态；优先 install 文件中的 cat2bug.upgrade，否则读侧车 state 文件。
     */
    public static Map<String, Object> readUpgradeState(String installConfigPath, String upgradeStatePath)
    {
        Map<String, Object> fromInstall = readUpgradeSectionFromFile(resolveConfigPath(installConfigPath));
        if (!fromInstall.isEmpty())
        {
            return fromInstall;
        }
        return readUpgradeSectionFromFile(resolveConfigPath(upgradeStatePath));
    }

    /**
     * 写入升级状态；若 install 文件已存在则写入 install，否则写入侧车 state 文件。
     */
    public static void writeUpgradeState(String configPath, Map<String, Object> upgradeState) throws IOException
    {
        writeUpgradeState(configPath, resolveUpgradeStatePath(configPath, null).toString(), upgradeState);
    }

    /**
     * 写入升级状态；若 install 文件已存在则写入 install，否则写入侧车 state 文件。
     */
    public static void writeUpgradeState(
            String installConfigPath,
            String upgradeStatePath,
            Map<String, Object> upgradeState) throws IOException
    {
        Path installPath = resolveConfigPath(installConfigPath);
        if (Files.isRegularFile(installPath))
        {
            Map<String, Object> root = loadYamlMap(installPath);
            if (root == null)
            {
                root = new LinkedHashMap<>();
            }
            setUpgradeSection(root, upgradeState);
            writeInstallConfig(installPath, root);
            return;
        }
        Path statePath = resolveConfigPath(upgradeStatePath);
        Map<String, Object> root = new LinkedHashMap<>();
        setUpgradeSection(root, upgradeState);
        writeInstallConfig(statePath, root);
    }

    public static Path resolveUpgradeStatePath(String installConfigPath, String customStatePath)
    {
        if (customStatePath != null && !customStatePath.isBlank())
        {
            return resolveConfigPath(customStatePath);
        }
        Path installPath = resolveConfigPath(installConfigPath);
        Path parent = installPath.getParent();
        if (parent == null)
        {
            return resolveConfigPath(DEFAULT_UPGRADE_STATE_PATH);
        }
        return parent.resolve("application-upgrade-state.yml").toAbsolutePath().normalize();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> readUpgradeSectionFromFile(Path path)
    {
        Map<String, Object> root = loadYamlMap(path);
        if (root == null)
        {
            return Map.of();
        }
        Object cat2bug = root.get("cat2bug");
        if (!(cat2bug instanceof Map<?, ?> cat2bugMap))
        {
            return Map.of();
        }
        Object upgrade = cat2bugMap.get("upgrade");
        if (!(upgrade instanceof Map<?, ?> upgradeMap))
        {
            return Map.of();
        }
        return InstallConfigMerge.deepCopy((Map<String, Object>) upgradeMap);
    }

    @SuppressWarnings("unchecked")
    private static void setUpgradeSection(Map<String, Object> root, Map<String, Object> upgradeState)
    {
        Map<String, Object> cat2bug = (Map<String, Object>) root.computeIfAbsent("cat2bug", key -> new LinkedHashMap<>());
        cat2bug.put("upgrade", InstallConfigMerge.deepCopy(upgradeState));
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> loadYamlMap(Path path)
    {
        if (!Files.isRegularFile(path))
        {
            return null;
        }
        try (InputStream in = Files.newInputStream(path))
        {
            Object loaded = new Yaml().load(in);
            if (loaded instanceof Map<?, ?> map)
            {
                return InstallConfigMerge.deepCopy((Map<String, Object>) map);
            }
        }
        catch (Exception ignored)
        {
            // fall through
        }
        return null;
    }
}
