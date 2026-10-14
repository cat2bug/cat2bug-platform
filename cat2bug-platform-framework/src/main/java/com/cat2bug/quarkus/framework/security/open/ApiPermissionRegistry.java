package com.cat2bug.quarkus.framework.security.open;

import com.cat2bug.quarkus.common.util.StringUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Open API 功能权限映射，对齐 Spring {@code ApiPermissionFilter}。
 */
public final class ApiPermissionRegistry {

    private static final Map<String, Map<String, String>> PERMISSION_MAPPING = new LinkedHashMap<>();

    static {
        PERMISSION_MAPPING.put("defect", Map.ofEntries(
                Map.entry("GET:/api/defect", "defect.list"),
                Map.entry("GET:/api/defect/{id}", "defect.query"),
                Map.entry("POST:/api/defect", "defect.create"),
                Map.entry("PUT:/api/defect", "defect.update"),
                Map.entry("DELETE:/api/defect", "defect.delete"),
                Map.entry("POST:/api/defect/{id}/assign", "defect.assign"),
                Map.entry("POST:/api/defect/{id}/reject", "defect.reject"),
                Map.entry("POST:/api/defect/{id}/repair", "defect.repair"),
                Map.entry("POST:/api/defect/{id}/pass", "defect.pass"),
                Map.entry("POST:/api/defect/{id}/close", "defect.close"),
                Map.entry("POST:/api/defect/{id}/open", "defect.open")
        ));
        PERMISSION_MAPPING.put("case", Map.ofEntries(
                Map.entry("GET:/api/case", "case.list"),
                Map.entry("GET:/api/case/{id}", "case.query"),
                Map.entry("POST:/api/case", "case.create"),
                Map.entry("PUT:/api/case", "case.update"),
                Map.entry("DELETE:/api/case/{id}", "case.delete")
        ));
        PERMISSION_MAPPING.put("deliverable", Map.ofEntries(
                Map.entry("GET:/api/deliverable", "deliverable.list"),
                Map.entry("GET:/api/deliverable/info", "deliverable.query"),
                Map.entry("POST:/api/deliverable", "deliverable.create")
        ));
        PERMISSION_MAPPING.put("file", Map.of(
                "POST:/api/file/upload", "file.upload"
        ));
        PERMISSION_MAPPING.put("project", Map.of(
                "GET:/api/project", "project.info",
                "GET:/api/member", "project.members"
        ));
    }

    private ApiPermissionRegistry() {
    }

    public static String requiredPermission(String method, String requestPath) {
        String normalizedPath = normalizePath(requestPath);
        String key = method.toUpperCase() + ":" + normalizedPath;
        for (Map<String, String> permissions : PERMISSION_MAPPING.values()) {
            if (permissions.containsKey(key)) {
                return permissions.get(key);
            }
        }
        return null;
    }

    public static boolean hasFeaturePermission(String featuresJson, String requiredPermission, ObjectMapper objectMapper) {
        if (StringUtils.isEmpty(requiredPermission)) {
            return true;
        }
        if (StringUtils.isEmpty(featuresJson)) {
            return true;
        }
        try {
            JsonNode features = objectMapper.readTree(featuresJson);
            String[] parts = requiredPermission.split("\\.");
            if (parts.length != 2) {
                return false;
            }
            JsonNode moduleNode = features.get(parts[0]);
            if (moduleNode == null || !moduleNode.isArray()) {
                return false;
            }
            for (JsonNode action : moduleNode) {
                if (parts[1].equals(action.asText())) {
                    return true;
                }
            }
            return false;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static String normalizePath(String path) {
        if (path == null) {
            return "";
        }
        String normalized = path.startsWith("/") ? path : "/" + path;
        return normalized.replaceAll("/\\d+", "/{id}");
    }
}
