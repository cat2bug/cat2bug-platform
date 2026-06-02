package com.cat2bug.system.util;

import com.cat2bug.system.domain.SysProjectDefectFieldManageItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 缺陷实体内置属性（含编号 projectNum），与列表 / Excel / 字段管理页一致。
 * <p>引用成员、交付物、测试用例等外键字段统一使用 {@code object} 类型（存 ID，展示名由业务解析）。</p>
 */
public final class DefectBuiltinFieldRegistry {

    /** 内置字段中不允许关闭「启用」、列表/表单必须展示的 fieldKey */
    public static final Set<String> ALWAYS_ENABLED_FIELD_KEYS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("defectName", "handleBy")));

    private static final List<BuiltinDef> DEFINITIONS;

    static {
        List<BuiltinDef> list = new ArrayList<>();
        list.add(def("projectNum", "id", "number", 0, 1, null, 0));
        int order = 10;
        list.add(def("defectType", "type", "enum", 1, 0, null, order));
        order += 10;
        list.add(def("defectName", "defect.name", "string", 1, 0, 1024, order));
        order += 10;
        list.add(def("handleBy", "handle-by", "object", 1, 0, null, order));
        order += 10;
        list.add(def("defectLevel", "priority", "enum", 0, 1, null, order));
        order += 10;
        list.add(def("defectState", "state", "enum", 0, 1, null, order));
        order += 10;
        list.add(def("moduleId", "module", "object", 0, 1, null, order));
        order += 10;
        list.add(def("moduleVersion", "version", "string", 0, 1, 128, order));
        order += 10;
        list.add(def("caseId", "case", "object", 0, 1, null, order));
        order += 10;
        list.add(def("defectDescribe", "describe", "text", 0, 1, 65536, order));
        order += 10;
        list.add(def("imgUrls", "image", "image", 0, 1, null, order));
        order += 10;
        list.add(def("annexUrls", "annex", "file", 0, 1, null, order));
        order += 10;
        list.add(def("planStartTime", "plan-start-time", "datetime", 0, 1, null, order));
        order += 10;
        list.add(def("planEndTime", "plan-end-time", "datetime", 0, 1, null, order));
        order += 10;
        list.add(def("createMember", "createBy", "object", 0, 1, null, order));
        order += 10;
        list.add(def("updateTime", "update-time", "datetime", 0, 1, null, order));
        DEFINITIONS = Collections.unmodifiableList(list);
    }

    private DefectBuiltinFieldRegistry() {
    }

    private static BuiltinDef def(
            String fieldKey,
            String labelKey,
            String fieldType,
            int required,
            int nullable,
            Integer maxLength,
            int defaultSort) {
        BuiltinDef d = new BuiltinDef();
        d.fieldKey = fieldKey;
        d.labelKey = labelKey;
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
        public String labelKey;
        public String fieldType;
        public int required;
        public int nullable;
        public Integer maxLength;
        public int defaultSort;
    }
}
