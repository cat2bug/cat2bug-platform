package com.cat2bug.system.util;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.cat2bug.common.exception.ServiceException;
import com.cat2bug.common.utils.MessageUtils;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.system.domain.SysProjectDefectField;
import com.cat2bug.system.mapper.SysProjectDefectFieldMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * 按项目已启用字段定义校验缺陷 custom_fields，并剥离未定义键。
 */
@Component
public class DefectCustomFieldsValidator {

    @Autowired
    private SysProjectDefectFieldMapper fieldMapper;

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
        for (Map.Entry<String, Object> e : input.entrySet()) {
            if (allowedKeys.contains(e.getKey())) {
                normalized.put(e.getKey(), e.getValue());
            }
        }

        for (SysProjectDefectField def : definitions) {
            Object value = normalized.get(def.getFieldKey());
            if (isRequired(def) && isEmptyValue(value)) {
                throw new ServiceException(MessageUtils.message(
                        "defect.field.value_required", def.getFieldLabel()));
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

    /**
     * 校验字段定义上的默认值，并规范为与 custom_fields 一致的存储形态。
     */
    public Object normalizeDefaultValue(SysProjectDefectField def, Object value) {
        if (isEmptyValue(value)) {
            return null;
        }
        return normalizeAndValidateValue(def, value);
    }

    private Object normalizeAndValidateValue(SysProjectDefectField def, Object value) {
        String type = def.getFieldType();
        JSONObject cfg = def.getTypeConfig() == null
                ? new JSONObject() : new JSONObject(def.getTypeConfig());
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
                if (!(value instanceof String) || StringUtils.isBlank((String) value)) {
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
            throw new ServiceException(MessageUtils.message(
                    "defect.field.value_too_long", def.getFieldLabel(), def.getMaxLength()));
        }
    }

    private static void validateNumber(SysProjectDefectField def, Object value) {
        if (!(value instanceof Number)) {
            throw fieldError(def);
        }
    }

    private void validateEnum(SysProjectDefectField def, Object value, JSONObject cfg) {
        if (!(value instanceof String)) {
            throw fieldError(def);
        }
        Set<String> keys = enumOptionKeys(cfg);
        if (!keys.contains(value)) {
            throw fieldError(def);
        }
    }

    private static void validateObject(SysProjectDefectField def, Object value) {
        if (value instanceof Map || value instanceof JSONObject) {
            return;
        }
        if (value instanceof String) {
            try {
                JSON.parseObject((String) value);
                return;
            } catch (Exception ignored) {
                // fall through
            }
        }
        throw fieldError(def);
    }

    /**
     * 图片/附件：存 string[]。兼容前端 ImageUpload 逗号分隔字符串及 JSONArray。
     */
    private List<String> normalizeUrlList(SysProjectDefectField def, Object value) {
        List<String> urls = parseUrlList(value);
        if (urls == null) {
            throw fieldError(def);
        }
        for (String item : urls) {
            if (StringUtils.isBlank(item)) {
                throw fieldError(def);
            }
        }
        return urls;
    }

    private static List<String> parseUrlList(Object value) {
        if (value instanceof String) {
            String s = ((String) value).trim();
            if (StringUtils.isBlank(s)) {
                return Collections.emptyList();
            }
            return Arrays.stream(s.split(","))
                    .map(String::trim)
                    .filter(StringUtils::isNotEmpty)
                    .collect(Collectors.toList());
        }
        if (value instanceof JSONArray) {
            JSONArray arr = (JSONArray) value;
            List<String> list = new ArrayList<>(arr.size());
            for (int i = 0; i < arr.size(); i++) {
                Object item = arr.get(i);
                if (item != null) {
                    String u = String.valueOf(item).trim();
                    if (StringUtils.isNotEmpty(u)) {
                        list.add(u);
                    }
                }
            }
            return list;
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

    private void validateArray(SysProjectDefectField def, Object value, JSONObject cfg) {
        if (!(value instanceof List)) {
            throw fieldError(def);
        }
        String itemType = cfg.getString("itemType");
        if (StringUtils.isBlank(itemType)) {
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

    private static Set<String> enumOptionKeys(JSONObject cfg) {
        Set<String> keys = new HashSet<>();
        JSONArray options = cfg.getJSONArray("options");
        if (options != null) {
            for (int i = 0; i < options.size(); i++) {
                JSONObject opt = options.getJSONObject(i);
                if (opt != null && opt.getString("key") != null) {
                    keys.add(opt.getString("key"));
                }
            }
        }
        return keys;
    }

    private static ServiceException fieldError(SysProjectDefectField def) {
        return new ServiceException(MessageUtils.message(
                "defect.field.value_invalid_for", def.getFieldLabel()));
    }
}
