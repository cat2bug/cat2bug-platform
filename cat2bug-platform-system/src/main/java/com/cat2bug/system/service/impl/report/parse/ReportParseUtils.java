package com.cat2bug.system.service.impl.report.parse;

import com.alibaba.fastjson.JSON;
import com.cat2bug.common.utils.StringUtils;

import java.util.Collections;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shared helpers for report DSL materialization.
 */
public final class ReportParseUtils {
    private static final Pattern STYLE_ATTR =
            Pattern.compile("\\[\\s*\"style\"\\s*:\\s*\"([^\"]*)\"\\s*\\]");
    private static final Pattern PROJECT_ICON_INDEX =
            Pattern.compile("^\\s*(\\d{1,2})\\s*$");

    private ReportParseUtils() {
    }

    public static boolean containsReportDsl(String content) {
        if (StringUtils.isBlank(content)) {
            return false;
        }
        return content.contains("$image")
                || content.contains("$img")
                || content.contains("${")
                || content.contains("$table")
                || content.contains("$card")
                || content.contains("$t{")
                || content.contains("$text{");
    }

    public static String readBeanField(Object bean, String fieldName) {
        if (bean == null) {
            return " ";
        }
        if (StringUtils.isBlank(fieldName)) {
            return JSON.toJSONString(bean);
        }
        try {
            if (bean instanceof Map) {
                Object value = ((Map<?, ?>) bean).get(fieldName);
                return value == null ? " " : String.valueOf(value);
            }
            // Stay on Fastjson for round-trip — mixing Jackson can drop fields / fail on enums.
            Object json = JSON.toJSON(bean);
            if (json instanceof Map) {
                Object value = ((Map<?, ?>) json).get(fieldName);
                return value == null ? " " : String.valueOf(value);
            }
            return " ";
        } catch (Exception e) {
            return " ";
        }
    }

    public static String extractStyleAttribute(String attrChunk) {
        if (StringUtils.isBlank(attrChunk)) {
            return "";
        }
        Matcher matcher = STYLE_ATTR.matcher(attrChunk);
        if (matcher.find()) {
            return matcher.group(1) == null ? "" : matcher.group(1);
        }
        return "";
    }

    /**
     * Resolve image src for report snapshot. Preset project icons become /static paths;
     * relative uploads and /static/img assets are prefixed with vueAppBaseApi when provided
     * so Vite-dev frontends load them via the API proxy.
     */
    public static String resolveImageSrc(String raw, String fieldKey, Map<String, Object> param) {
        if (StringUtils.isBlank(raw)) {
            return "";
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")
                || trimmed.startsWith("data:") || trimmed.startsWith("//")) {
            return trimmed;
        }

        String base = "";
        if (param != null && param.get("vueAppBaseApi") != null) {
            base = String.valueOf(param.get("vueAppBaseApi"));
            if (base.endsWith("/")) {
                base = base.substring(0, base.length() - 1);
            }
        }

        boolean projectIcon = "projectIcon".equals(fieldKey)
                || (fieldKey != null && fieldKey.endsWith("projectIcon"));
        if (projectIcon) {
            Matcher indexMatcher = PROJECT_ICON_INDEX.matcher(trimmed);
            if (indexMatcher.matches()) {
                int index = Integer.parseInt(indexMatcher.group(1));
                if (index >= 1 && index <= 10) {
                    return withBaseApi("/static/img/project_icon" + index + ".svg", base);
                }
            }
            if (trimmed.startsWith("/static/") || trimmed.startsWith("/assets/")) {
                return withBaseApi(trimmed, base);
            }
            if (trimmed.matches("(?i).*project_icon\\d{1,2}(?:\\.[a-f0-9]+)?\\.svg.*")) {
                Matcher m = Pattern.compile("(?i)project_icon(\\d{1,2})").matcher(trimmed);
                if (m.find()) {
                    return withBaseApi("/static/img/project_icon" + m.group(1) + ".svg", base);
                }
            }
        }

        if (trimmed.startsWith("/static/") || trimmed.startsWith("/assets/")) {
            return withBaseApi(trimmed, base);
        }

        if (StringUtils.isNotBlank(base)) {
            if (trimmed.startsWith(base)) {
                return trimmed;
            }
            if (trimmed.startsWith("/")) {
                return base + trimmed;
            }
            return base + "/" + trimmed;
        }
        return trimmed.startsWith("/") ? trimmed : "/" + trimmed;
    }

    private static String withBaseApi(String path, String base) {
        if (StringUtils.isBlank(path)) {
            return path;
        }
        if (StringUtils.isBlank(base)) {
            return path;
        }
        if (path.startsWith(base)) {
            return path;
        }
        if (path.startsWith("/")) {
            return base + path;
        }
        return base + "/" + path;
    }

    public static Map<String, Object> safeParams(Map<String, Object> param) {
        return param == null ? Collections.emptyMap() : param;
    }

    public static String quoteReplacement(String replacement) {
        return Matcher.quoteReplacement(replacement == null ? "" : replacement);
    }

    public static String escapeHtmlAttr(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
