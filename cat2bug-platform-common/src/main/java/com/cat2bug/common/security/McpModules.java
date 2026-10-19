package com.cat2bug.common.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 项目 MCP 可勾选的模块及其覆盖的权限串前缀。
 * <p>
 * 不在任何模块里的权限（用户、角色、部门、菜单、字典、参数、岗位、定时任务、监控、团队、项目 API Key、
 * 新建 / 删除 / 推送项目等）一律视为系统级，MCP 中不可用。新增模块追加到末尾即可：已有项目保存的是
 * 「已勾选列表」，新模块对它们默认不勾选。
 */
public final class McpModules {

    public record Module(String key, List<String> permissionPrefixes) {
    }

    public static final List<Module> ALL = List.of(
            new Module("defect", List.of("system:defect:", "system:comment:")),
            new Module("case", List.of("system:case:")),
            new Module("plan", List.of("system:plan:", "system:PlanItem:")),
            new Module("deliverable", List.of("system:module:")),
            new Module("document", List.of("system:document:")),
            new Module("code", List.of("system:git:")),
            new Module("dashboard", List.of("system:dashboard:")),
            new Module("project", List.of("system:project:query", "system:project:list", "system:project:edit",
                    "system:project:member", "system:project:defect-field:", "system:project:case-type:")));

    private McpModules() {
    }

    public static List<String> allKeys() {
        List<String> keys = new ArrayList<>();
        for (Module module : ALL) {
            keys.add(module.key());
        }
        return keys;
    }

    public static boolean isKnown(String key) {
        return find(key) != null;
    }

    public static Module find(String key) {
        for (Module module : ALL) {
            if (module.key().equals(key)) {
                return module;
            }
        }
        return null;
    }

    /** 权限串所属模块键；系统级权限返回 {@code null}。 */
    public static String moduleOf(String permission) {
        if (permission == null) {
            return null;
        }
        for (Module module : ALL) {
            for (String prefix : module.permissionPrefixes()) {
                if (permission.startsWith(prefix)) {
                    return module.key();
                }
            }
        }
        return null;
    }

    /** 过滤掉未知模块键并去重，保持 {@link #ALL} 的顺序。 */
    public static List<String> normalize(Collection<String> keys) {
        Set<String> wanted = keys == null ? Set.of() : new LinkedHashSet<>(keys);
        List<String> out = new ArrayList<>();
        for (Module module : ALL) {
            if (wanted.contains(module.key())) {
                out.add(module.key());
            }
        }
        return out;
    }
}
