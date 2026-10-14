package com.cat2bug.system.util;

import com.cat2bug.common.domain.SysProjectDefectField;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.common.util.StringUtils;
import com.cat2bug.system.mapper.SysProjectDefectFieldMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 按项目已启用字段定义校验缺陷 custom_fields 默认值。
 */
@ApplicationScoped
public class DefectCustomFieldsValidator {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Inject
    SysProjectDefectFieldMapper fieldMapper;

    /**
     * 校验并返回仅含已启用 field_key 的 map（剥离未定义/已停用键）。
     */
    public Map<String, Object> validateAndStrip(Long projectId, Map<String, Object> customFields) {
        List<SysProjectDefectField> definitions = fieldMapper.selectEnabledListByProjectId(projectId);
        Set<String> allowedKeys = definitions.stream()
                .map(SysProjectDefectField::getFieldKey)
                .collect(Collectors.toSet());

        Map<String, Object> input = customFields != null ? customFields : new LinkedHashMap<>();
        Map<String, Object> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : input.entrySet()) {
            if (allowedKeys.contains(entry.getKey())) {
                normalized.put(entry.getKey(), entry.getValue());
            }
        }

        for (SysProjectDefectField def : definitions) {
            Object value = normalized.get(def.getFieldKey());
            if (isRequired(def) && isEmptyValue(value)) {
                throw new ApiException("自定义字段「" + def.getFieldLabel() + "」不能为空");
            }
            if (value != null && !isEmptyValue(value)) {
                normalized.put(def.getFieldKey(), normalizeAndValidateValue(def, value));
            } else if (!isRequired(def) && value != null && isEmptyValue(value)) {
                normalized.remove(def.getFieldKey());
            }
        }
        return normalized;
    }

    private static boolean isRequired(SysProjectDefectField def) {
        return def.getRequired() != null && def.getRequired() == 1;
    }

    public Object normalizeDefaultValue(SysProjectDefectField def, Object value) {
        if (isEmptyValue(value)) {
            return null;
        }
        return normalizeAndValidateValue(def, value);
    }

    private Object normalizeAndValidateValue(SysProjectDefectField def, Object value) {
        String type = def.getFieldType();
        Map<String, Object> cfg = def.getTypeConfig() != null ? def.getTypeConfig() : Collections.emptyMap();
        switch (type) {
            case "string":
                validateString(def, value);
                return value;
            case "number":
                validateNumber(def, value);
                return value;
            case "boolean":
                if (!(value instanceof Boolean)) {
                    throw fieldError(def);
                }
                return value;
            case "datetime":
                if (!(value instanceof String) || ((String) value).isBlank()) {
                    throw fieldError(def);
                }
                return value;
            case "enum":
                validateEnum(def, value, cfg);
                return value;
            case "object":
                validateObject(def, value);
                return value;
            case "image":
            case "file":
                return normalizeUrlList(def, value);
            case "array":
                validateArray(def, value, cfg);
                return value;
            default:
                throw fieldError(def);
        }
    }

    private void validateString(SysProjectDefectField def, Object value) {
        if (!(value instanceof String)) {
            throw fieldError(def);
        }
        String s = (String) value;
        if (def.getMaxLength() != null && def.getMaxLength() > 0 && s.length() > def.getMaxLength()) {
            throw new ApiException("自定义字段「" + def.getFieldLabel() + "」超过最大长度 " + def.getMaxLength());
        }
    }

    private static void validateNumber(SysProjectDefectField def, Object value) {
        if (!(value instanceof Number)) {
            throw fieldError(def);
        }
    }

    private void validateEnum(SysProjectDefectField def, Object value, Map<String, Object> cfg) {
        if (!(value instanceof String)) {
            throw fieldError(def);
        }
        Set<String> keys = enumOptionKeys(cfg);
        if (!keys.contains(value)) {
            throw fieldError(def);
        }
    }

    private static void validateObject(SysProjectDefectField def, Object value) {
        if (value instanceof Map) {
            return;
        }
        if (value instanceof String) {
            try {
                MAPPER.readTree((String) value);
                return;
            } catch (Exception ignored) {
                // fall through
            }
        }
        throw fieldError(def);
    }

    private List<String> normalizeUrlList(SysProjectDefectField def, Object value) {
        List<String> urls = parseUrlList(value);
        if (urls == null) {
            throw fieldError(def);
        }
        for (String item : urls) {
            if (StringUtils.isEmpty(item)) {
                throw fieldError(def);
            }
        }
        return urls;
    }

    private static List<String> parseUrlList(Object value) {
        if (value instanceof String) {
            String s = ((String) value).trim();
            if (s.isEmpty()) {
                return Collections.emptyList();
            }
            return Arrays.stream(s.split(","))
                    .map(String::trim)
                    .filter(StringUtils::isNotEmpty)
                    .collect(Collectors.toList());
        }
        if (value instanceof List) {
            List<String> list = new ArrayList<>();
            for (Object item : (List<?>) value) {
                if (item != null) {
                    String u = String.valueOf(item).trim();
                    if (StringUtils.isNotEmpty(u)) {
                        list.add(u);
                    }
                }
            }
            return list;
        }
        return null;
    }

    private void validateArray(SysProjectDefectField def, Object value, Map<String, Object> cfg) {
        if (!(value instanceof List)) {
            throw fieldError(def);
        }
        Object itemTypeObj = cfg.get("itemType");
        String itemType = itemTypeObj != null ? String.valueOf(itemTypeObj) : null;
        if (StringUtils.isEmpty(itemType)) {
            throw fieldError(def);
        }
        SysProjectDefectField itemDef = new SysProjectDefectField();
        itemDef.setFieldLabel(def.getFieldLabel());
        itemDef.setFieldType(itemType);
        itemDef.setTypeConfig(cfg);
        for (Object item : (List<?>) value) {
            if (item == null || isEmptyValue(item)) {
                continue;
            }
            normalizeAndValidateValue(itemDef, item);
        }
    }

    @SuppressWarnings("unchecked")
    private static Set<String> enumOptionKeys(Map<String, Object> cfg) {
        Set<String> keys = new HashSet<>();
        Object optionsObj = cfg.get("options");
        if (optionsObj instanceof List) {
            for (Object optObj : (List<?>) optionsObj) {
                if (optObj instanceof Map) {
                    Map<String, Object> opt = (Map<String, Object>) optObj;
                    Object key = opt.get("key");
                    if (key != null) {
                        keys.add(String.valueOf(key));
                    }
                }
            }
        }
        return keys;
    }

    private static boolean isEmptyValue(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String) {
            return ((String) value).isBlank();
        }
        if (value instanceof List) {
            return ((List<?>) value).isEmpty();
        }
        if (value instanceof Map) {
            return ((Map<?, ?>) value).isEmpty();
        }
        return false;
    }

    private static ApiException fieldError(SysProjectDefectField def) {
        return new ApiException("自定义字段「" + def.getFieldLabel() + "」值无效");
    }
}
