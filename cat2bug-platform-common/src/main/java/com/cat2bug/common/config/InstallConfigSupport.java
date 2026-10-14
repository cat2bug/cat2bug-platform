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
 * 安装配置文件路径解析与读写。
 */
public final class InstallConfigSupport {

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

    private InstallConfigSupport() {
    }

    public static Path resolveConfigPath(String configPath) {
        return locateExistingConfigFile(configPath).orElseGet(() -> resolveWorkingDirectoryConfigPath(configPath));
    }

    public static Path resolveWorkingDirectoryConfigPath(String configPath) {
        String path = normalizeConfigPath(configPath);
        return Paths.get(path).toAbsolutePath().normalize();
    }

    public static boolean isConfigFilePresent(String configPath) {
        return locateExistingConfigFile(configPath).isPresent();
    }

    public static Optional<Path> locateExistingConfigFile(String configPath) {
        return locateExistingConfigFile(configPath, resolveJarDirectory());
    }

    /**
     * 定位安装向导写入的 {@code quarkus-runtime.properties}（与 install YAML 同目录，并兼容 JAR 在 {@code target/} 时从模块根查找）。
     */
    public static Optional<Path> locateRuntimePropertiesFile(String configPath) {
        Optional<Path> installFile = locateExistingConfigFile(configPath);
        if (installFile.isPresent()) {
            Path runtime = installFile.get().getParent().resolve("quarkus-runtime.properties");
            if (Files.isRegularFile(runtime)) {
                return Optional.of(runtime.toAbsolutePath().normalize());
            }
        }
        Path workingDirectoryCandidate = resolveWorkingDirectoryConfigPath(configPath)
                .getParent()
                .resolve("quarkus-runtime.properties");
        if (Files.isRegularFile(workingDirectoryCandidate)) {
            return Optional.of(workingDirectoryCandidate.toAbsolutePath().normalize());
        }
        Path jarDirectory = resolveJarDirectory();
        if (jarDirectory != null) {
            Path inJarDirectory = jarDirectory.resolve("config/install/quarkus-runtime.properties").normalize();
            if (Files.isRegularFile(inJarDirectory)) {
                return Optional.of(inJarDirectory.toAbsolutePath().normalize());
            }
            Path moduleRoot = jarDirectory.getParent();
            if (moduleRoot != null) {
                Path inModuleRoot = moduleRoot.resolve("config/install/quarkus-runtime.properties").normalize();
                if (Files.isRegularFile(inModuleRoot)) {
                    return Optional.of(inModuleRoot.toAbsolutePath().normalize());
                }
            }
        }
        return Optional.empty();
    }

    static Optional<Path> locateExistingConfigFile(String configPath, Path jarDirectory) {
        String path = normalizeConfigPath(configPath);
        Path workingDirectoryCandidate = Paths.get(path).toAbsolutePath().normalize();
        if (Files.isRegularFile(workingDirectoryCandidate)) {
            return Optional.of(workingDirectoryCandidate);
        }
        if (Paths.get(path).isAbsolute() || jarDirectory == null) {
            return Optional.empty();
        }
        Path jarDirectoryCandidate = jarDirectory.resolve(path).toAbsolutePath().normalize();
        if (Files.isRegularFile(jarDirectoryCandidate)) {
            return Optional.of(jarDirectoryCandidate);
        }
        return Optional.empty();
    }

    public static Path resolveJarDirectory() {
        Path fromCodeSource = resolveJarDirectoryFromCodeSource();
        if (fromCodeSource != null) {
            return fromCodeSource;
        }
        return resolveJarDirectoryFromClassPath();
    }

    private static Path resolveJarDirectoryFromCodeSource() {
        try {
            var protectionDomain = InstallConfigSupport.class.getProtectionDomain();
            if (protectionDomain == null || protectionDomain.getCodeSource() == null) {
                return null;
            }
            Path codePath = toLocalPath(protectionDomain.getCodeSource().getLocation().toURI());
            if (codePath == null) {
                return null;
            }
            codePath = codePath.toAbsolutePath().normalize();
            if (Files.isRegularFile(codePath)) {
                Path parent = codePath.getParent();
                return parent != null ? parent.toAbsolutePath().normalize() : null;
            }
            if (Files.isDirectory(codePath)) {
                Path parent = codePath.getParent();
                return parent != null ? parent.toAbsolutePath().normalize() : null;
            }
        } catch (URISyntaxException | SecurityException ignored) {
            return null;
        }
        return null;
    }

    private static Path resolveJarDirectoryFromClassPath() {
        String classPath = System.getProperty("java.class.path", "");
        if (classPath.isBlank() || classPath.contains(java.io.File.pathSeparator)) {
            return null;
        }
        Path jarFile = Paths.get(classPath).toAbsolutePath().normalize();
        if (!Files.isRegularFile(jarFile) || !jarFile.getFileName().toString().endsWith(".jar")) {
            return null;
        }
        Path parent = jarFile.getParent();
        return parent != null ? parent.toAbsolutePath().normalize() : null;
    }

    static Path toLocalPath(URI location) throws URISyntaxException {
        if (location == null) {
            return null;
        }
        if ("file".equalsIgnoreCase(location.getScheme())) {
            return Paths.get(location);
        }
        if ("jar".equalsIgnoreCase(location.getScheme())) {
            String spec = location.getRawSchemeSpecificPart();
            int separator = spec.indexOf('!');
            String fileUri = separator >= 0 ? spec.substring(0, separator) : spec;
            return Paths.get(URI.create(fileUri));
        }
        return null;
    }

    private static String normalizeConfigPath(String configPath) {
        return configPath != null && !configPath.isBlank() ? configPath : DEFAULT_CONFIG_PATH;
    }

    public static boolean isInstallSkipped(String skipProperty) {
        return "true".equalsIgnoreCase(skipProperty)
                || "true".equalsIgnoreCase(System.getenv("CAT2BUG_INSTALL_SKIP"));
    }

    public static boolean isInstallCompletedOnDisk(String configPath) {
        Path path = resolveConfigPath(configPath);
        if (!Files.isRegularFile(path)) {
            return false;
        }
        try (InputStream in = Files.newInputStream(path)) {
            Object loaded = new Yaml().load(in);
            if (!(loaded instanceof Map<?, ?> root)) {
                return false;
            }
            return readCompletedFlag(root);
        } catch (Exception ignored) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private static boolean readCompletedFlag(Map<?, ?> root) {
        Object cat2bug = root.get("cat2bug");
        if (!(cat2bug instanceof Map<?, ?> installRoot)) {
            return false;
        }
        Object install = installRoot.get("install");
        if (!(install instanceof Map<?, ?> installSection)) {
            return false;
        }
        Object completed = installSection.get("completed");
        if (completed instanceof Boolean bool) {
            return bool;
        }
        if (completed != null) {
            return "true".equalsIgnoreCase(String.valueOf(completed).trim());
        }
        return false;
    }

    public static void writeInstallConfig(Path target, Map<String, Object> root) throws IOException {
        Files.createDirectories(target.getParent());
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        String content = new Yaml(options).dump(root);
        Files.writeString(target, content, StandardCharsets.UTF_8);
    }

    @SuppressWarnings("unchecked")
    public static void setInstallCompleted(Map<String, Object> root, boolean completed) {
        Map<String, Object> cat2bug = (Map<String, Object>) root.computeIfAbsent("cat2bug", key -> new LinkedHashMap<>());
        Map<String, Object> install = (Map<String, Object>) cat2bug.computeIfAbsent("install", key -> new LinkedHashMap<>());
        install.put("completed", completed);
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> loadYamlMap(java.nio.file.Path path) {
        if (!java.nio.file.Files.isRegularFile(path)) {
            return null;
        }
        try (java.io.InputStream in = java.nio.file.Files.newInputStream(path)) {
            Object loaded = new org.yaml.snakeyaml.Yaml().load(in);
            if (loaded instanceof Map<?, ?> map) {
                return InstallConfigMerge.deepCopy((Map<String, Object>) map);
            }
        } catch (Exception ignored) {
            // fall through
        }
        return null;
    }

    public static Map<String, Object> readUpgradeState(String configPath) {
        return readUpgradeState(configPath, resolveUpgradeStatePath(configPath, null).toString());
    }

    public static Map<String, Object> readUpgradeState(String installConfigPath, String upgradeStatePath) {
        Map<String, Object> fromInstall = readUpgradeSectionFromFile(resolveConfigPath(installConfigPath));
        if (!fromInstall.isEmpty()) {
            return fromInstall;
        }
        return readUpgradeSectionFromFile(resolveConfigPath(upgradeStatePath));
    }

    public static void writeUpgradeState(String configPath, Map<String, Object> upgradeState) throws IOException {
        writeUpgradeState(configPath, resolveUpgradeStatePath(configPath, null).toString(), upgradeState);
    }

    public static void writeUpgradeState(
            String installConfigPath,
            String upgradeStatePath,
            Map<String, Object> upgradeState) throws IOException {
        Path installPath = resolveConfigPath(installConfigPath);
        if (Files.isRegularFile(installPath)) {
            Map<String, Object> root = loadYamlMap(installPath);
            if (root == null) {
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

    public static Path resolveUpgradeStatePath(String installConfigPath, String customStatePath) {
        if (customStatePath != null && !customStatePath.isBlank()) {
            return resolveConfigPath(customStatePath);
        }
        Path installPath = resolveConfigPath(installConfigPath);
        Path parent = installPath.getParent();
        if (parent == null) {
            return resolveConfigPath(DEFAULT_UPGRADE_STATE_PATH);
        }
        return parent.resolve("application-upgrade-state.yml").toAbsolutePath().normalize();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> readUpgradeSectionFromFile(Path path) {
        Map<String, Object> root = loadYamlMap(path);
        if (root == null) {
            return Map.of();
        }
        Object cat2bug = root.get("cat2bug");
        if (!(cat2bug instanceof Map<?, ?> cat2bugMap)) {
            return Map.of();
        }
        Object upgrade = cat2bugMap.get("upgrade");
        if (!(upgrade instanceof Map<?, ?> upgradeMap)) {
            return Map.of();
        }
        return InstallConfigMerge.deepCopy((Map<String, Object>) upgradeMap);
    }

    @SuppressWarnings("unchecked")
    private static void setUpgradeSection(Map<String, Object> root, Map<String, Object> upgradeState) {
        Map<String, Object> cat2bug = (Map<String, Object>) root.computeIfAbsent("cat2bug", key -> new LinkedHashMap<>());
        cat2bug.put("upgrade", InstallConfigMerge.deepCopy(upgradeState));
    }
}
