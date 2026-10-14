package com.cat2bug.system.util;

import com.cat2bug.common.domain.SysProjectDefectFieldManageItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 缺陷实体内置属性（含编号 projectNum），与列表 / Excel / 字段管理页一致。
 */
public final class DefectBuiltinFieldRegistry {

    public static final Set<String> ALWAYS_ENABLED_FIELD_KEYS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("defectName", "handleBy")));

    private static final Map<String, String> BUILTIN_LABELS;

    private static final List<BuiltinDef> DEFINITIONS;

    static {
        Map<String, String> labels = new HashMap<>();
        labels.put("projectNum", "缺陷编号");
        labels.put("defectType", "缺陷类型");
        labels.put("defectName", "缺陷名称");
        labels.put("handleBy", "处理人");
        labels.put("defectLevel", "优先级");
        labels.put("defectState", "状态");
        labels.put("moduleId", "交付物");
        labels.put("moduleVersion", "版本");
        labels.put("caseId", "测试用例");
        labels.put("defectDescribe", "描述");
        labels.put("imgUrls", "图片");
        labels.put("annexUrls", "附件");
        labels.put("planStartTime", "计划开始时间");
        labels.put("planEndTime", "计划结束时间");
        labels.put("createMember", "创建人");
        labels.put("updateTime", "更新时间");
        BUILTIN_LABELS = Collections.unmodifiableMap(labels);

        List<BuiltinDef> list = new ArrayList<>();
        list.add(def("projectNum", "number", 0, 1, null, 0));
        int order = 10;
        list.add(def("defectType", "enum", 1, 0, null, order));
        order += 10;
        list.add(def("defectName", "string", 1, 0, 1024, order));
        order += 10;
        list.add(def("handleBy", "object", 1, 0, null, order));
        order += 10;
        list.add(def("defectLevel", "enum", 0, 1, null, order));
        order += 10;
        list.add(def("defectState", "enum", 0, 1, null, order));
        order += 10;
        list.add(def("moduleId", "object", 0, 1, null, order));
        order += 10;
        list.add(def("moduleVersion", "string", 0, 1, 128, order));
        order += 10;
        list.add(def("caseId", "object", 0, 1, null, order));
        order += 10;
        list.add(def("defectDescribe", "text", 0, 1, 65536, order));
        order += 10;
        list.add(def("imgUrls", "image", 0, 1, null, order));
        order += 10;
        list.add(def("annexUrls", "file", 0, 1, null, order));
        order += 10;
        list.add(def("planStartTime", "datetime", 0, 1, null, order));
        order += 10;
        list.add(def("planEndTime", "datetime", 0, 1, null, order));
        order += 10;
        list.add(def("createMember", "object", 0, 1, null, order));
        order += 10;
        list.add(def("updateTime", "datetime", 0, 1, null, order));
        DEFINITIONS = Collections.unmodifiableList(list);
    }

    private DefectBuiltinFieldRegistry() {
    }

    private static BuiltinDef def(
            String fieldKey,
            String fieldType,
            int required,
            int nullable,
            Integer maxLength,
            int defaultSort) {
        BuiltinDef d = new BuiltinDef();
        d.fieldKey = fieldKey;
        d.fieldType = fieldType;
        d.required = required;
        d.nullable = nullable;
        d.maxLength = maxLength;
        d.defaultSort = defaultSort;
        return d;
    }

    public static List<BuiltinDef> definitions() {
        return DEFINITIONS;
    }

    public static Map<String, BuiltinDef> definitionByKey() {
        Map<String, BuiltinDef> map = new LinkedHashMap<>();
        for (BuiltinDef d : DEFINITIONS) {
            map.put(d.fieldKey, d);
        }
        return map;
    }

    public static boolean isAlwaysEnabled(String fieldKey) {
        return fieldKey != null && ALWAYS_ENABLED_FIELD_KEYS.contains(fieldKey);
    }

    public static String resolveBuiltinLabel(String fieldKey) {
        return BUILTIN_LABELS.getOrDefault(fieldKey, fieldKey);
    }

    public static SysProjectDefectFieldManageItem toManageItem(
            BuiltinDef def,
            String fieldLabel,
            int enabled,
            int sortOrder) {
        SysProjectDefectFieldManageItem item = new SysProjectDefectFieldManageItem();
        item.setSystemBuiltin(true);
        item.setFieldId(null);
        item.setFieldKey(def.fieldKey);
        item.setFieldLabel(fieldLabel);
        item.setFieldType(def.fieldType);
        item.setMaxLength(def.maxLength);
        item.setRequired(def.required);
        item.setNullable(def.nullable);
        item.setEnabled(enabled);
        item.setSortOrder(sortOrder);
        return item;
    }

    public static class BuiltinDef {
        public String fieldKey;
        public String fieldType;
        public int required;
        public int nullable;
        public Integer maxLength;
        public int defaultSort;
    }
}
