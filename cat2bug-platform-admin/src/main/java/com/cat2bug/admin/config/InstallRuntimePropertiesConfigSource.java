package com.cat2bug.admin.config;

import com.cat2bug.admin.setup.QuarkusInstallRuntimePropertiesWriter;
import com.cat2bug.common.config.InstallConfigSupport;
import org.eclipse.microprofile.config.spi.ConfigSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

/**
 * 自动加载 {@code config/install/quarkus-runtime.properties}，避免依赖启动脚本注入
 * {@code quarkus.config.locations}（安装后须激活 {@code runtime-mysql} 等运行期属性）。
 */
public final class InstallRuntimePropertiesConfigSource implements ConfigSource {

    static final int ORDINAL = 450;

    private static final String NAME = "cat2bug-install-runtime-properties";

    /** surefire 设置；ConfigSource 构造早于 Quarkus 测试 profile 生效，只能靠系统属性识别测试进程。 */
    static final String SKIP_PROPERTY = "cat2bug.install.runtime.skip";

    private final Map<String, String> properties;

    public InstallRuntimePropertiesConfigSource() {
        this.properties = loadProperties();
    }

    static Map<String, String> loadProperties() {
        if (isQuarkusTestProfile()) {
            return Map.of();
        }
        Optional<Path> runtimeFile = InstallConfigSupport.locateRuntimePropertiesFile(
                InstallConfigSupport.DEFAULT_CONFIG_PATH);
        if (runtimeFile.isEmpty()) {
            return Map.of();
        }
        Properties loaded = new Properties();
        try (InputStream in = Files.newInputStream(runtimeFile.get())) {
            loaded.load(in);
        } catch (IOException ignored) {
            return Map.of();
        }
        return toNormalizedMap(loaded);
    }

    /** QuarkusTest 使用独立 test 配置，勿加载磁盘 install/runtime（避免污染 setup/upgrade 断言）。 */
    static boolean isQuarkusTestProfile() {
        if (Boolean.getBoolean(SKIP_PROPERTY)) {
            return true;
        }
        String profile = System.getProperty("quarkus.profile");
        if (profile == null || profile.isBlank()) {
            profile = System.getenv("QUARKUS_PROFILE");
        }
        return "test".equalsIgnoreCase(profile);
    }

    /** 仅从指定路径加载（单测用，不走 JAR 目录回退）。 */
    static Map<String, String> loadPropertiesFromPath(Path runtimeFile) {
        if (runtimeFile == null || !Files.isRegularFile(runtimeFile)) {
            return Map.of();
        }
        Properties loaded = new Properties();
        try (InputStream in = Files.newInputStream(runtimeFile)) {
            loaded.load(in);
        } catch (IOException ignored) {
            return Map.of();
        }
        return toNormalizedMap(loaded);
    }

    /** H2 相对文件路径统一解析为绝对路径（Native 下 H2 相对路径基准不可靠）。 */
    private static Map<String, String> toNormalizedMap(Properties loaded) {
        Map<String, String> result = new HashMap<>();
        for (String key : loaded.stringPropertyNames()) {
            String value = loaded.getProperty(key);
            if (key.endsWith(".jdbc.url")) {
                value = QuarkusInstallRuntimePropertiesWriter.absolutizeH2FileUrl(value);
            }
            result.put(key, value);
        }
        return Collections.unmodifiableMap(result);
    }

    @Override
    public Map<String, String> getProperties() {
        return properties;
    }

    @Override
    public String getValue(String propertyName) {
        return properties.get(propertyName);
    }

    @Override
    public Set<String> getPropertyNames() {
        return properties.keySet();
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int getOrdinal() {
        return ORDINAL;
    }
}
