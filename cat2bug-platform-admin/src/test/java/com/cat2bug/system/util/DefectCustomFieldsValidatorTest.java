package com.cat2bug.system.util;

import com.cat2bug.common.domain.SysProjectDefectField;
import com.cat2bug.common.exception.ApiException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefectCustomFieldsValidatorTest {

    private final DefectCustomFieldsValidator validator = new DefectCustomFieldsValidator();

    @Test
    void booleanInvalidMessageIncludesExample() {
        SysProjectDefectField def = field("布尔", "boolean");
        ApiException ex = assertThrows(ApiException.class, () -> validator.normalizeDefaultValue(def, "maybe"));
        assertEquals("自定义字段「布尔」值无效（例如：true、false）", ex.getMessage());
    }

    @Test
    void booleanAcceptsCommonAliases() {
        SysProjectDefectField def = field("布尔", "boolean");
        assertEquals(Boolean.TRUE, validator.normalizeDefaultValue(def, "True"));
        assertEquals(Boolean.FALSE, validator.normalizeDefaultValue(def, "否"));
        assertEquals(Boolean.TRUE, validator.normalizeDefaultValue(def, 1));
    }

    @Test
    void enumInvalidMessageIncludesOptionLabels() {
        SysProjectDefectField def = field("优先级", "enum");
        def.setTypeConfig(Map.of("options", List.of(
                Map.of("key", "high", "label", "高"),
                Map.of("key", "low", "label", "低")
        )));
        ApiException ex = assertThrows(ApiException.class, () -> validator.normalizeDefaultValue(def, "中"));
        assertTrue(ex.getMessage().contains("例如：高、低"));
    }

    private static SysProjectDefectField field(String label, String type) {
        SysProjectDefectField def = new SysProjectDefectField();
        def.setFieldLabel(label);
        def.setFieldType(type);
        return def;
    }
}
