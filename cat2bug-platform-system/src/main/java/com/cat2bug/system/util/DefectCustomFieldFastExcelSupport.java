package com.cat2bug.system.util;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.cat2bug.common.core.domain.entity.SysDefect;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.common.utils.poi.ExcelExportColumnParams;
import com.cat2bug.system.domain.SysProjectDefectField;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 缺陷自定义字段 FastExcel 路径（无 Apache POI 依赖，供 Native 与 B9 SPI 使用）。
 */
public final class DefectCustomFieldFastExcelSupport {

    public static final String CUSTOM_COLUMN_KEY_PREFIX = "custom:";
    public static final String CUSTOM_COLUMN_PROP_PREFIX = "custom_";

    private DefectCustomFieldFastExcelSupport() {
    }
    /**
     * 从二维表文本合并自定义字段（B9 FastExcel：无 POI 依赖）。
     */
    public static void mergeCustomFieldsFromRows(List<List<String>> rows, List<SysDefect> defects,
            List<SysProjectDefectField> fieldDefinitions) {
        if (rows == null || rows.isEmpty() || defects == null || defects.isEmpty()
                || fieldDefinitions == null || fieldDefinitions.isEmpty()) {
            return;
        }
        int headerRowIndex = findHeaderRowIndex(rows);
        if (headerRowIndex < 0 || headerRowIndex >= rows.size()) {
            return;
        }
        List<String> headerCells = rows.get(headerRowIndex);
        Map<Integer, SysProjectDefectField> colIndexToDef = mapCustomColumnsByHeader(headerCells, fieldDefinitions);
        if (colIndexToDef.isEmpty()) {
            return;
        }
        for (int r = headerRowIndex + 1; r < rows.size(); r++) {
            List<String> row = rows.get(r);
            if (row == null || isEmptyTextRow(row)) {
                continue;
            }
            int defectIndex = r - headerRowIndex - 1;
            if (defectIndex < 0 || defectIndex >= defects.size()) {
                break;
            }
            SysDefect defect = defects.get(defectIndex);
            Map<String, Object> custom = defect.getCustomFields() != null
                    ? new LinkedHashMap<>(defect.getCustomFields()) : new LinkedHashMap<>();
            for (Map.Entry<Integer, SysProjectDefectField> e : colIndexToDef.entrySet()) {
                String text = cellText(row, e.getKey());
                Object parsed = parseImportCellValue(text, e.getValue());
                if (parsed != null && !isEmptyValue(parsed)) {
                    custom.put(e.getValue().getFieldKey(), parsed);
                }
            }
            if (!custom.isEmpty()) {
                defect.setCustomFields(custom);
            }
        }
    }

    /**
     * B9：供 {@link com.cat2bug.web.service.excel.DefectExcelColumnSupport} 读取列顺序。
     */
    public static List<ColumnSlot> buildOrderedSlots(Map<String, Object> params, Map<String, String> propToField,
            Set<String> requiredFields, Set<String> templateExcludedProps) {
        return buildOrderedSlotsInternal(params, propToField, requiredFields, templateExcludedProps);
    }

    private static List<ColumnSlot> buildOrderedSlotsInternal(Map<String, Object> params, Map<String, String> propToField,
            Set<String> requiredFields, Set<String> templateExcludedProps) {
        com.alibaba.fastjson2.JSONArray columns = parseExportColumns(params.get(ExcelExportColumnParams.PARAM_EXPORT_COLUMNS));
        if (columns == null || columns.isEmpty()) {
            return Collections.emptyList();
        }
        String scope = String.valueOf(params.getOrDefault(ExcelExportColumnParams.PARAM_EXPORT_SCOPE,
                ExcelExportColumnParams.SCOPE_DATA));
        if (requiredFields == null) {
            requiredFields = Collections.emptySet();
        }
        if (templateExcludedProps == null) {
            templateExcludedProps = Collections.emptySet();
        }
        List<ColumnSlot> slots = new ArrayList<>();
        for (int i = 0; i < columns.size(); i++) {
            com.alibaba.fastjson2.JSONObject col = columns.getJSONObject(i);
            if (col == null) {
                continue;
            }
            String prop = col.getString("prop");
            String key = col.getString("key");
            boolean visible = col.getBooleanValue("visible");
            if (ExcelExportColumnParams.SCOPE_IMPORT_TEMPLATE.equals(scope)
                    && (templateExcludedProps.contains(prop) || templateExcludedProps.contains(key))) {
                continue;
            }
            String fieldKey = resolveCustomFieldKey(prop, key);
            if (fieldKey != null) {
                boolean include = ExcelExportColumnParams.SCOPE_IMPORT_TEMPLATE.equals(scope)
                        ? visible || isCustomRequired(col)
                        : visible;
                if (include) {
                    slots.add(ColumnSlot.custom(fieldKey));
                }
                continue;
            }
            String fieldName = resolveStandardFieldName(propToField, prop, key);
            if (fieldName == null) {
                continue;
            }
            boolean include;
            if (ExcelExportColumnParams.SCOPE_IMPORT_TEMPLATE.equals(scope)) {
                include = visible || requiredFields.contains(fieldName);
            } else {
                include = visible;
            }
            if (include) {
                slots.add(ColumnSlot.standard(fieldName));
            }
        }
        return slots;
    }

    private static boolean isCustomRequired(com.alibaba.fastjson2.JSONObject col) {
        return col != null && col.getBooleanValue("required");
    }

    private static String resolveStandardFieldName(Map<String, String> propToField, String prop, String key) {
        if (StringUtils.isNotEmpty(prop) && propToField.containsKey(prop)) {
            return propToField.get(prop);
        }
        if (StringUtils.isNotEmpty(key) && propToField.containsKey(key)) {
            return propToField.get(key);
        }
        return null;
    }

    public static String resolveCustomFieldKey(String prop, String key) {
        if (StringUtils.isNotEmpty(key) && key.startsWith(CUSTOM_COLUMN_KEY_PREFIX)) {
            return key.substring(CUSTOM_COLUMN_KEY_PREFIX.length());
        }
        if (StringUtils.isNotEmpty(prop) && prop.startsWith(CUSTOM_COLUMN_PROP_PREFIX)) {
            return prop.substring(CUSTOM_COLUMN_PROP_PREFIX.length());
        }
        if (StringUtils.isNotEmpty(prop) && prop.startsWith(CUSTOM_COLUMN_KEY_PREFIX)) {
            return prop.substring(CUSTOM_COLUMN_KEY_PREFIX.length());
        }
        return null;
    }

    private static com.alibaba.fastjson2.JSONArray parseExportColumns(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof com.alibaba.fastjson2.JSONArray) {
            return (com.alibaba.fastjson2.JSONArray) raw;
        }
        if (raw instanceof String) {
            String s = ((String) raw).trim();
            if (StringUtils.isEmpty(s)) {
                return null;
            }
            return com.alibaba.fastjson2.JSON.parseArray(s);
        }
        if (raw instanceof java.util.Collection) {
            return new com.alibaba.fastjson2.JSONArray((java.util.Collection<?>) raw);
        }
        return com.alibaba.fastjson2.JSON.parseArray(com.alibaba.fastjson2.JSON.toJSONString(raw));
    }

    public static String formatExportValue(Object value, SysProjectDefectField def) {
        if (value == null || isEmptyValue(value)) {
            return "";
        }
        String type = def.getFieldType();
        if ("enum".equals(type)) {
            JSONObject cfg = def.getTypeConfig() == null ? new JSONObject() : new JSONObject(def.getTypeConfig());
            JSONArray options = cfg.getJSONArray("options");
            String sk = String.valueOf(value);
            if (options != null) {
                for (int i = 0; i < options.size(); i++) {
                    JSONObject opt = options.getJSONObject(i);
                    if (opt != null && sk.equals(String.valueOf(opt.get("key")))) {
                        Object label = opt.get("label");
                        return label != null ? String.valueOf(label) : sk;
                    }
                }
            }
            return sk;
        }
        if ("boolean".equals(type)) {
            if (value instanceof Boolean) {
                return ((Boolean) value) ? "true" : "false";
            }
            return String.valueOf(value);
        }
        if ("image".equals(type) || "file".equals(type) || "array".equals(type)) {
            if (value instanceof List) {
                return ((List<?>) value).stream().map(String::valueOf).collect(Collectors.joining(", "));
            }
            return String.valueOf(value);
        }
        if ("object".equals(type)) {
            if (value instanceof Map || value instanceof JSONObject) {
                return com.alibaba.fastjson2.JSON.toJSONString(value);
            }
            return String.valueOf(value);
        }
        return String.valueOf(value);
    }

    public static Object parseImportCellValue(String raw, SysProjectDefectField def) {
        if (StringUtils.isBlank(raw)) {
            return null;
        }
        String text = raw.trim();
        String type = def.getFieldType();
        if ("enum".equals(type)) {
            JSONObject cfg = def.getTypeConfig() == null ? new JSONObject() : new JSONObject(def.getTypeConfig());
            JSONArray options = cfg.getJSONArray("options");
            if (options != null) {
                for (int i = 0; i < options.size(); i++) {
                    JSONObject opt = options.getJSONObject(i);
                    if (opt == null) {
                        continue;
                    }
                    String key = opt.getString("key");
                    String label = opt.getString("label");
                    if (text.equals(key) || (label != null && text.equals(label))) {
                        return key;
                    }
                }
            }
            return text;
        }
        if ("boolean".equals(type)) {
            if ("true".equalsIgnoreCase(text) || "1".equals(text)) {
                return true;
            }
            if ("false".equalsIgnoreCase(text) || "0".equals(text)) {
                return false;
            }
            return text;
        }
        if ("number".equals(type)) {
            try {
                if (text.contains(".")) {
                    return Double.parseDouble(text);
                }
                return Long.parseLong(text);
            } catch (NumberFormatException e) {
                return text;
            }
        }
        if ("image".equals(type) || "file".equals(type) || "array".equals(type)) {
            String[] parts = text.split("[,，]");
            List<String> list = new ArrayList<>();
            for (String p : parts) {
                String s = p.trim();
                if (StringUtils.isNotEmpty(s)) {
                    list.add(s);
                }
            }
            return list.isEmpty() ? null : list;
        }
        return text;
    }

    private static Map<Integer, SysProjectDefectField> mapCustomColumnsByHeader(List<String> headerCells,
            List<SysProjectDefectField> fieldDefinitions) {
        Map<Integer, SysProjectDefectField> map = new HashMap<>();
        Set<String> standardHeaders = buildStandardImportHeaderTexts();
        Map<String, SysProjectDefectField> byKey = new HashMap<>();
        Map<String, SysProjectDefectField> byLabel = new HashMap<>();
        for (SysProjectDefectField def : fieldDefinitions) {
            if (def == null) {
                continue;
            }
            if (StringUtils.isNotEmpty(def.getFieldKey())) {
                byKey.put(def.getFieldKey().trim(), def);
            }
            if (StringUtils.isNotEmpty(def.getFieldLabel())) {
                byLabel.put(def.getFieldLabel().trim(), def);
            }
        }
        for (int c = 0; c < headerCells.size(); c++) {
            String headerText = cellText(headerCells, c);
            if (StringUtils.isEmpty(headerText) || standardHeaders.contains(headerText)) {
                continue;
            }
            SysProjectDefectField def = byKey.get(headerText);
            if (def == null) {
                def = byLabel.get(headerText);
            }
            if (def != null) {
                map.put(c, def);
            }
        }
        return map;
    }

    /** 下拉显示枚举 label（无 label 时用 key） */
    public static String[] resolveEnumComboLabels(SysProjectDefectField def) {
        if (def == null || !"enum".equals(def.getFieldType())) {
            return new String[0];
        }
        JSONObject cfg = def.getTypeConfig() == null ? new JSONObject() : new JSONObject(def.getTypeConfig());
        JSONArray options = cfg.getJSONArray("options");
        if (options == null || options.isEmpty()) {
            return new String[0];
        }
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < options.size(); i++) {
            JSONObject opt = options.getJSONObject(i);
            if (opt == null) {
                continue;
            }
            Object label = opt.get("label");
            String text = label != null && StringUtils.isNotEmpty(String.valueOf(label))
                    ? String.valueOf(label).trim()
                    : String.valueOf(opt.get("key")).trim();
            if (StringUtils.isNotEmpty(text) && !labels.contains(text)) {
                labels.add(text);
            }
        }
        return labels.toArray(new String[0]);
    }

    private static Set<String> buildStandardImportHeaderTexts() {
        Set<String> set = new HashSet<>();
        set.add(com.cat2bug.common.utils.MessageUtils.message("type"));
        set.add(com.cat2bug.common.utils.MessageUtils.message("defect.name"));
        set.add(com.cat2bug.common.utils.MessageUtils.message("describe"));
        set.add(com.cat2bug.common.utils.MessageUtils.message("defect.level"));
        set.add(com.cat2bug.common.utils.MessageUtils.message("defect.state"));
        set.add(com.cat2bug.common.utils.MessageUtils.message("module"));
        set.add(com.cat2bug.common.utils.MessageUtils.message("version"));
        set.add(com.cat2bug.common.utils.MessageUtils.message("image"));
        set.add(com.cat2bug.common.utils.MessageUtils.message("handle-by"));
        return set;
    }

    private static int findHeaderRowIndex(List<List<String>> rows) {
        int limit = Math.min(5, rows.size());
        for (int r = 0; r < limit; r++) {
            List<String> row = rows.get(r);
            if (row == null) {
                continue;
            }
            for (String cell : row) {
                if (StringUtils.isNotEmpty(cell)) {
                    return r;
                }
            }
        }
        return 0;
    }

    private static boolean isEmptyTextRow(List<String> row) {
        if (row == null) {
            return true;
        }
        for (String cell : row) {
            if (StringUtils.isNotBlank(cell)) {
                return false;
            }
        }
        return true;
    }

    private static String cellText(List<String> row, int columnIndex) {
        if (row == null || columnIndex < 0 || columnIndex >= row.size()) {
            return "";
        }
        String value = row.get(columnIndex);
        return value == null ? "" : value.trim();
    }

    private static boolean isEmptyValue(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String) {
            return StringUtils.isBlank((String) value);
        }
        if (value instanceof List) {
            return ((List<?>) value).isEmpty();
        }
        if (value instanceof Map) {
            return ((Map<?, ?>) value).isEmpty();
        }
        return false;
    }

    public static final class ColumnSlot {
        private final boolean custom;
        private final String fieldKey;
        private final String standardFieldName;

        private ColumnSlot(boolean custom, String fieldKey, String standardFieldName) {
            this.custom = custom;
            this.fieldKey = fieldKey;
            this.standardFieldName = standardFieldName;
        }

        public static ColumnSlot custom(String fieldKey) {
            return new ColumnSlot(true, fieldKey, null);
        }

        public static ColumnSlot standard(String fieldName) {
            return new ColumnSlot(false, null, fieldName);
        }

        public boolean isCustom() {
            return custom;
        }

        public String getFieldKey() {
            return fieldKey;
        }

        public String getStandardFieldName() {
            return standardFieldName;
        }
    }
}
