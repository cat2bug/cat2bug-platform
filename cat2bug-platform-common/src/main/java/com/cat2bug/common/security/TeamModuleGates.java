package com.cat2bug.common.security;

/**
 * 权限前缀 / 路径 / MCP 模块键 → 团队开关键。
 * 开关键与 {@link McpModules} 对齐：{@code case_plan} 同时覆盖 case 与 plan。
 */
public final class TeamModuleGates {

    public static final String CASE_PLAN = "case_plan";
    public static final String DEFECT = "defect";
    public static final String DELIVERABLE = "deliverable";
    public static final String DOCUMENT = "document";
    public static final String CODE = "code";

    private TeamModuleGates() {
    }

    /** MCP 模块键对应的团队开关键；仪表盘、项目等无闸门时返回 {@code null}。 */
    public static String gateOfMcpKey(String mcpKey) {
        if (mcpKey == null) {
            return null;
        }
        return switch (mcpKey) {
            case "case", "plan" -> CASE_PLAN;
            case "defect" -> DEFECT;
            case "deliverable" -> DELIVERABLE;
            case "document" -> DOCUMENT;
            case "code" -> CODE;
            default -> null;
        };
    }

    /**
     * 权限串所属团队开关键。
     * {@code system:team:} 以及项目级配置（用例类型、缺陷字段等，落在 project 模块）不设闸门。
     */
    public static String gateOfPermission(String permission) {
        if (permission == null || permission.isBlank() || permission.startsWith("system:team:")) {
            return null;
        }
        if (permission.startsWith("system:case-review:") || permission.contains(":case-review")) {
            return CASE_PLAN;
        }
        return gateOfMcpKey(McpModules.moduleOf(permission));
    }

    /** 无权限注解时按资源路径归类。路径为去掉前导斜杠的形式。 */
    public static String gateOfPath(String path) {
        if (path == null || path.isBlank() || exempt(path)) {
            return null;
        }
        if (path.startsWith("system/case-review")
                || path.startsWith("system/case/") || "system/case".equals(path)
                || path.startsWith("system/CasePrompt")
                || path.startsWith("system/plan")
                || path.startsWith("system/PlanItem")
                || path.startsWith("ai/case")
                || path.startsWith("cloud/api/case")
                || path.startsWith("api/case")) {
            return CASE_PLAN;
        }
        if (path.startsWith("system/defect")
                || path.startsWith("system/comment")
                || path.startsWith("system/log")
                || path.startsWith("ai/defect")
                || path.startsWith("api/defect")) {
            return DEFECT;
        }
        if (path.startsWith("system/module") || path.startsWith("api/deliverable")) {
            return DELIVERABLE;
        }
        if (path.startsWith("system/document")) {
            return DOCUMENT;
        }
        if (path.startsWith("system/git") || path.startsWith("git/p/") || isNamespacedGit(path)) {
            return CODE;
        }
        return null;
    }

    /** 团队设置、项目 MCP / 类型 / 字段等预配置入口不拦截，便于关闭后重新打开。 */
    public static boolean exempt(String path) {
        if (path == null) {
            return false;
        }
        if (path.startsWith("system/team/") || path.startsWith("admin/")) {
            return true;
        }
        if (path.contains("/module-page-settings")) {
            return true;
        }
        return path.matches("system/project/\\d+/(mcp|defect-fields|case-types|case-review-template)(/.*)?");
    }

    /**
     * 权限注解映射出的应拒绝开关键。
     * AND：任一权限落在已关闭开关键上则拒绝。
     * OR：仅当每个权限都落在已关闭开关键上才拒绝（存在无闸门或仍开启的权限则放行）。
     *
     * @param enabled 开关键当前是否启用
     * @return 应拒绝的开关键；放行时 {@code null}
     */
    public static String closedGate(boolean or, String[] permissions, java.util.function.Predicate<String> enabled) {
        if (permissions == null || permissions.length == 0) {
            return null;
        }
        if (or) {
            boolean anyPass = false;
            String closed = null;
            for (String permission : permissions) {
                String gate = gateOfPermission(permission);
                if (gate == null || enabled.test(gate)) {
                    anyPass = true;
                } else if (closed == null) {
                    closed = gate;
                }
            }
            return anyPass ? null : closed;
        }
        for (String permission : permissions) {
            String gate = gateOfPermission(permission);
            if (gate != null && !enabled.test(gate)) {
                return gate;
            }
        }
        return null;
    }

    private static boolean isNamespacedGit(String path) {
        return path.matches("[^/]+/[^/]+/code/[^/]+\\.git(/.*)?");
    }
}
