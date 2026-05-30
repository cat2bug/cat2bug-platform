package com.cat2bug.common.config;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 升级状态读写（install 文件 {@code cat2bug.upgrade} 段或侧车 YAML）。
 */
public final class UpgradeSupport
{
    public static final String STATE_PENDING = InstallConfigSupport.UPGRADE_STATE_PENDING;
    public static final String STATE_RUNNING = InstallConfigSupport.UPGRADE_STATE_RUNNING;
    public static final String STATE_FAILED = InstallConfigSupport.UPGRADE_STATE_FAILED;
    public static final String STATE_RESTART_REQUIRED = InstallConfigSupport.UPGRADE_STATE_RESTART_REQUIRED;
    public static final String STATE_COMPLETED = InstallConfigSupport.UPGRADE_STATE_COMPLETED;

    public static final String STEP_BACKUP = InstallConfigSupport.UPGRADE_LAST_STEP_BACKUP;

    public static final String STEP_CONFIG = InstallConfigSupport.UPGRADE_LAST_STEP_CONFIG;

    public static final String STEP_MIGRATION = InstallConfigSupport.UPGRADE_LAST_STEP_MIGRATION;

    private static final Set<String> ACTIVE_STATES = Set.of(
            STATE_PENDING, STATE_RUNNING, STATE_FAILED, STATE_RESTART_REQUIRED);

    private UpgradeSupport()
    {
    }

    public static boolean isActiveState(String state)
    {
        return state != null && ACTIVE_STATES.contains(state);
    }

    public static void writeUpgradeSection(Path installConfigPath, Map<String, Object> upgradeSection) throws Exception
    {
        Path parent = installConfigPath.getParent();
        if (parent != null)
        {
            Files.createDirectories(parent);
        }
        Map<String, Object> root = new LinkedHashMap<>();
        if (Files.isRegularFile(installConfigPath))
        {
            Object loaded = new Yaml().load(Files.readString(installConfigPath));
            if (loaded instanceof Map<?, ?> existing)
            {
                @SuppressWarnings("unchecked")
                Map<String, Object> cast = (Map<String, Object>) existing;
                root.putAll(cast);
            }
        }
        Map<String, Object> cat2bug = nestedMap(root, "cat2bug");
        cat2bug.put("upgrade", upgradeSection);
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Files.writeString(installConfigPath, new Yaml(options).dump(root));
    }

    @SuppressWarnings("unchecked")
    public static String readState(Path installConfigPath)
    {
        if (!Files.isRegularFile(installConfigPath))
        {
            return InstallConfigSupport.UPGRADE_STATE_COMPLETED;
        }
        try
        {
            Object loaded = new Yaml().load(Files.readString(installConfigPath));
            if (!(loaded instanceof Map<?, ?> root))
            {
                return InstallConfigSupport.UPGRADE_STATE_COMPLETED;
            }
            Object cat2bug = root.get("cat2bug");
            if (!(cat2bug instanceof Map<?, ?> cat2bugMap))
            {
                return InstallConfigSupport.UPGRADE_STATE_COMPLETED;
            }
            Object upgrade = cat2bugMap.get("upgrade");
            if (!(upgrade instanceof Map<?, ?> upgradeMap))
            {
                return InstallConfigSupport.UPGRADE_STATE_COMPLETED;
            }
            Object state = upgradeMap.get("state");
            return state != null ? state.toString() : InstallConfigSupport.UPGRADE_STATE_COMPLETED;
        }
        catch (Exception ignored)
        {
            return InstallConfigSupport.UPGRADE_STATE_COMPLETED;
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> nestedMap(Map<String, Object> root, String key)
    {
        Object existing = root.get(key);
        if (existing instanceof Map<?, ?> map)
        {
            return (Map<String, Object>) map;
        }
        Map<String, Object> created = new LinkedHashMap<>();
        root.put(key, created);
        return created;
    }
}
