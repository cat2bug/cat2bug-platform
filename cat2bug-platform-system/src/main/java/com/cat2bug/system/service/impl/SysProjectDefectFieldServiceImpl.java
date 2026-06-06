package com.cat2bug.system.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.cat2bug.common.exception.ServiceException;
import com.cat2bug.common.utils.DateUtils;
import com.cat2bug.common.utils.MessageUtils;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.system.domain.SysProjectDefectField;
import com.cat2bug.system.domain.SysProjectDefectFieldColumnLayout;
import com.cat2bug.system.domain.SysProjectDefectFieldManageItem;
import com.cat2bug.system.mapper.SysProjectDefectFieldMapper;
import com.cat2bug.system.service.ISysProjectDefectFieldService;
import com.cat2bug.system.util.DefectBuiltinFieldRegistry;
import com.cat2bug.system.util.DefectBuiltinFieldRegistry.BuiltinDef;
import com.cat2bug.system.util.DefectCustomFieldsValidator;
import com.google.common.base.Preconditions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 项目缺陷自定义字段定义 Service
 */
@Service
public class SysProjectDefectFieldServiceImpl implements ISysProjectDefectFieldService {

    public static final int MAX_ENABLED_FIELDS = 50;

    /** 自定义字段显示名称最大长度（字符数） */
    public static final int MAX_FIELD_LABEL_LENGTH = 6;

    public static final Pattern FIELD_KEY_PATTERN = Pattern.compile("^[a-z][a-z0-9_]{0,63}$");

    private static final Set<String> FIELD_TYPES = new HashSet<>(Arrays.asList(
            "string", "number", "boolean", "datetime", "enum", "object", "image", "file", "array"
    ));

    private static final Set<String> ARRAY_ITEM_TYPES = new HashSet<>(Arrays.asList(
            "string", "number", "boolean", "datetime", "enum", "object", "image", "file"
    ));

    /** 字段管理页不支持配置默认值的类型 */
    private static final Set<String> NO_DEFAULT_VALUE_TYPES = new HashSet<>(Arrays.asList(
            "image", "file"
    ));

    @Autowired
    private SysProjectDefectFieldMapper fieldMapper;

    @Autowired
    private DefectCustomFieldsValidator customFieldsValidator;

    @Override
    public List<SysProjectDefectField> selectListByProjectId(Long projectId) {
        return fieldMapper.selectListByProjectId(projectId);
    }

    @Override
    public List<SysProjectDefectFieldManageItem> selectManageListByProjectId(Long projectId) {
        Map<String, BuiltinDef> defs = DefectBuiltinFieldRegistry.definitionByKey();
        JSONObject config = loadBuiltinConfigObject(projectId);
        List<SysProjectDefectFieldManageItem> items = new ArrayList<>();

        for (BuiltinDef def : DefectBuiltinFieldRegistry.definitions()) {
            JSONObject entry = config.getJSONObject(def.fieldKey);
            int enabled = entry != null && entry.containsKey("enabled") ? entry.getIntValue("enabled") : 1;
            if (DefectBuiltinFieldRegistry.isAlwaysEnabled(def.fieldKey)) {
                enabled = 1;
            }
            int sortOrder = entry != null && entry.containsKey("sortOrder")
                    ? entry.getIntValue("sortOrder") : def.defaultSort;
            items.add(DefectBuiltinFieldRegistry.toManageItem(
                    def, resolveBuiltinLabel(def.fieldKey), enabled, sortOrder));
        }

        for (SysProjectDefectField field : fieldMapper.selectListByProjectId(projectId)) {
            items.add(toCustomManageItem(field));
        }

        items.sort(Comparator
                .comparing(SysProjectDefectFieldManageItem::getSortOrder,
                        Comparator.nullsLast(Integer::compareTo))
                .thenComparing(item -> item.getFieldKey() != null ? item.getFieldKey() : "",
                        Comparator.nullsLast(String::compareTo)));
        return items;
    }

    @Override
    public int updateBuiltinFieldLayout(Long projectId, List<SysProjectDefectFieldManageItem> layout) {
        if (layout == null || layout.isEmpty()) {
            return 0;
        }
        Map<String, BuiltinDef> defs = DefectBuiltinFieldRegistry.definitionByKey();
        JSONObject config = loadBuiltinConfigObject(projectId);
        for (SysProjectDefectFieldManageItem item : layout) {
            if (item == null || StringUtils.isBlank(item.getFieldKey()) || !defs.containsKey(item.getFieldKey())) {
                continue;
            }
            JSONObject entry = config.getJSONObject(item.getFieldKey());
            if (entry == null) {
                entry = new JSONObject();
                config.put(item.getFieldKey(), entry);
            }
            if (item.getEnabled() != null) {
                int enabled = DefectBuiltinFieldRegistry.isAlwaysEnabled(item.getFieldKey()) ? 1 : item.getEnabled();
                entry.put("enabled", enabled);
            }
            if (item.getSortOrder() != null) {
                entry.put("sortOrder", item.getSortOrder());
            }
        }
        return fieldMapper.updateDefectBuiltinFieldConfig(
                projectId, config.toJSONString(), DateUtils.getNowDate());
    }

    @Override
    public List<SysProjectDefectField> selectEnabledListByProjectId(Long projectId) {
        return fieldMapper.selectEnabledListByProjectId(projectId);
    }

    @Override
    public SysProjectDefectFieldColumnLayout selectColumnLayoutByProjectId(Long projectId) {
        JSONObject config = loadBuiltinConfigObject(projectId);
        List<String> enabledBuiltinKeys = new ArrayList<>();
        for (BuiltinDef def : DefectBuiltinFieldRegistry.definitions()) {
            if (DefectBuiltinFieldRegistry.isAlwaysEnabled(def.fieldKey)) {
                enabledBuiltinKeys.add(def.fieldKey);
                continue;
            }
            JSONObject entry = config.getJSONObject(def.fieldKey);
            if (entry != null && entry.getIntValue("enabled") == 1) {
                enabledBuiltinKeys.add(def.fieldKey);
            }
        }
        SysProjectDefectFieldColumnLayout layout = new SysProjectDefectFieldColumnLayout();
        layout.setEnabledBuiltinFieldKeys(enabledBuiltinKeys);
        layout.setCustomFields(fieldMapper.selectEnabledListByProjectId(projectId));
        List<String> orderedEnabledFieldKeys = new ArrayList<>();
        for (SysProjectDefectFieldManageItem item : selectManageListByProjectId(projectId)) {
            if (item.getEnabled() != null && item.getEnabled() == 1 && StringUtils.isNotBlank(item.getFieldKey())) {
                orderedEnabledFieldKeys.add(item.getFieldKey());
            }
        }
        layout.setOrderedEnabledFieldKeys(orderedEnabledFieldKeys);
        return layout;
    }

    @Override
    public SysProjectDefectField selectByFieldId(Long fieldId) {
        return fieldMapper.selectByFieldId(fieldId);
    }

    @Override
    public SysProjectDefectField insert(SysProjectDefectField field) {
        Preconditions.checkNotNull(field.getProjectId(), MessageUtils.message("defect.field.project_required"));
        normalizeDefaults(field);
        validateFieldKey(field.getFieldKey());
        validateFieldType(field.getFieldType());
        validateTypeConfig(field.getFieldType(), field.getTypeConfig());
        normalizeDefaultValue(field);

        if (fieldMapper.selectByProjectIdAndFieldKey(field.getProjectId(), field.getFieldKey()) != null) {
            throw new ServiceException(MessageUtils.message("defect.field.key_duplicate"));
        }
        if (DefectBuiltinFieldRegistry.definitionByKey().containsKey(field.getFieldKey())) {
            throw new ServiceException(MessageUtils.message("defect.field.key_reserved"));
        }
        assertFieldLabelLength(field.getFieldLabel());
        assertFieldLabelUnique(field.getProjectId(), null, field.getFieldLabel());
        assertEnabledLimit(field.getProjectId(), null, field.getEnabled());

        field.setCreateTime(DateUtils.getNowDate());
        field.setUpdateTime(field.getCreateTime());
        if (fieldMapper.insert(field) != 1) {
            throw new ServiceException(MessageUtils.message("defect.field.insert_fail"));
        }
        return field;
    }

    @Override
    public int update(SysProjectDefectField field) {
        Preconditions.checkNotNull(field.getFieldId(), MessageUtils.message("defect.field.id_required"));
        SysProjectDefectField existing = fieldMapper.selectByFieldId(field.getFieldId());
        if (existing == null || !existing.getProjectId().equals(field.getProjectId())) {
            throw new ServiceException(MessageUtils.message("defect.field.not_found"));
        }
        if (field.getFieldKey() != null && !field.getFieldKey().equals(existing.getFieldKey())) {
            throw new ServiceException(MessageUtils.message("defect.field.key_immutable"));
        }

        String newType = field.getFieldType() != null ? field.getFieldType() : existing.getFieldType();
        if (field.getFieldType() != null && !field.getFieldType().equals(existing.getFieldType())) {
            if (fieldMapper.countCustomFieldKeyUsed(existing.getProjectId(), existing.getFieldKey()) > 0) {
                throw new ServiceException(MessageUtils.message("defect.field.type_change_forbidden"));
            }
            validateFieldType(newType);
        }

        Map<String, Object> typeConfig = field.getTypeConfig() != null ? field.getTypeConfig() : existing.getTypeConfig();
        validateTypeConfig(newType, typeConfig);

        SysProjectDefectField defaultProbe = new SysProjectDefectField();
        defaultProbe.setFieldKey(existing.getFieldKey());
        defaultProbe.setFieldLabel(field.getFieldLabel() != null ? field.getFieldLabel() : existing.getFieldLabel());
        defaultProbe.setFieldType(newType);
        defaultProbe.setTypeConfig(typeConfig);
        defaultProbe.setMaxLength(field.getMaxLength() != null ? field.getMaxLength() : existing.getMaxLength());
        field.setDefaultValue(customFieldsValidator.normalizeDefaultValue(defaultProbe, field.getDefaultValue()));

        Integer newEnabled = field.getEnabled() != null ? field.getEnabled() : existing.getEnabled();
        assertEnabledLimit(existing.getProjectId(), existing.getFieldId(), newEnabled);

        String newLabel = field.getFieldLabel() != null ? field.getFieldLabel() : existing.getFieldLabel();
        assertFieldLabelLength(newLabel);
        assertFieldLabelUnique(existing.getProjectId(), existing.getFieldId(), newLabel);

        field.setFieldKey(existing.getFieldKey());
        field.setProjectId(existing.getProjectId());
        field.setUpdateTime(DateUtils.getNowDate());
        return fieldMapper.update(field);
    }

    @Override
    public int softDelete(Long projectId, Long fieldId) {
        SysProjectDefectField existing = fieldMapper.selectByFieldId(fieldId);
        if (existing == null || !existing.getProjectId().equals(projectId)) {
            throw new ServiceException(MessageUtils.message("defect.field.not_found"));
        }
        return fieldMapper.softDelete(fieldId, projectId, DateUtils.getNowDate());
    }

    private JSONObject loadBuiltinConfigObject(Long projectId) {
        String raw = fieldMapper.selectDefectBuiltinFieldConfig(projectId);
        JSONObject stored = StringUtils.isNotBlank(raw) ? JSON.parseObject(raw) : new JSONObject();
        JSONObject merged = new JSONObject();
        for (BuiltinDef def : DefectBuiltinFieldRegistry.definitions()) {
            JSONObject entry = stored.getJSONObject(def.fieldKey);
            int enabled = 1;
            int sortOrder = def.defaultSort;
            if (entry != null) {
                if (entry.containsKey("enabled")) {
                    enabled = entry.getIntValue("enabled");
                }
                if (entry.containsKey("sortOrder")) {
                    sortOrder = entry.getIntValue("sortOrder");
                }
            }
            if (DefectBuiltinFieldRegistry.isAlwaysEnabled(def.fieldKey)) {
                enabled = 1;
            }
            JSONObject normalized = new JSONObject();
            normalized.put("enabled", enabled);
            normalized.put("sortOrder", sortOrder);
            merged.put(def.fieldKey, normalized);
        }
        return merged;
    }

    private static SysProjectDefectFieldManageItem toCustomManageItem(SysProjectDefectField field) {
        SysProjectDefectFieldManageItem item = new SysProjectDefectFieldManageItem();
        item.setSystemBuiltin(false);
        item.setFieldId(field.getFieldId());
        item.setFieldKey(field.getFieldKey());
        item.setFieldLabel(field.getFieldLabel());
        item.setFieldType(field.getFieldType());
        item.setMaxLength(field.getMaxLength());
        item.setRequired(field.getRequired());
        item.setNullable(field.getNullable());
        item.setEnabled(field.getEnabled());
        item.setSortOrder(field.getSortOrder());
        item.setDefaultValue(field.getDefaultValue());
        item.setTypeConfig(field.getTypeConfig());
        return item;
    }

    private void normalizeDefaultValue(SysProjectDefectField field) {
        if (NO_DEFAULT_VALUE_TYPES.contains(field.getFieldType())) {
            field.setDefaultValue(null);
            return;
        }
        SysProjectDefectField probe = new SysProjectDefectField();
        probe.setFieldKey(field.getFieldKey());
        probe.setFieldLabel(field.getFieldLabel());
        probe.setFieldType(field.getFieldType());
        probe.setTypeConfig(field.getTypeConfig());
        probe.setMaxLength(field.getMaxLength());
        field.setDefaultValue(customFieldsValidator.normalizeDefaultValue(probe, field.getDefaultValue()));
    }

    private static String resolveBuiltinLabel(String fieldKey) {
        String key = "defect.builtin-field." + fieldKey;
        String label = MessageUtils.message(key);
        return key.equals(label) ? fieldKey : label;
    }

    private void normalizeDefaults(SysProjectDefectField field) {
        if (field.getRequired() == null) {
            field.setRequired(0);
        }
        if (field.getNullable() == null) {
            field.setNullable(1);
        }
        if (field.getEnabled() == null) {
            field.setEnabled(1);
        }
        if (field.getIsSystem() == null) {
            field.setIsSystem(0);
        }
        if (field.getSortOrder() == null) {
            field.setSortOrder(0);
        }
        if (field.getTypeConfig() == null) {
            field.setTypeConfig(new JSONObject());
        }
    }

    public static void validateFieldKey(String fieldKey) {
        if (StringUtils.isBlank(fieldKey) || !FIELD_KEY_PATTERN.matcher(fieldKey).matches()) {
            throw new ServiceException(MessageUtils.message("defect.field.key_invalid"));
        }
    }

    public static void validateFieldType(String fieldType) {
        if (StringUtils.isBlank(fieldType) || !FIELD_TYPES.contains(fieldType)) {
            throw new ServiceException(MessageUtils.message("defect.field.type_invalid"));
        }
    }

    /**
     * 校验 type_config（enum options、array itemType 等）。
     */
    public static void validateTypeConfig(String fieldType, Map<String, Object> typeConfig) {
        JSONObject cfg = typeConfig == null ? new JSONObject() : new JSONObject(typeConfig);
        if ("enum".equals(fieldType)) {
            validateEnumConfig(cfg);
        } else if ("array".equals(fieldType)) {
            validateArrayConfig(cfg);
        }
    }

    private static void validateEnumConfig(JSONObject cfg) {
        JSONArray options = cfg.getJSONArray("options");
        if (options == null || options.isEmpty()) {
            throw new ServiceException(MessageUtils.message("defect.field.enum_options_required"));
        }
        Set<String> keys = new HashSet<>();
        for (int i = 0; i < options.size(); i++) {
            JSONObject opt = options.getJSONObject(i);
            if (opt == null) {
                throw new ServiceException(MessageUtils.message("defect.field.enum_options_invalid"));
            }
            String key = opt.getString("key");
            if (StringUtils.isBlank(key) || !keys.add(key)) {
                throw new ServiceException(MessageUtils.message("defect.field.enum_options_invalid"));
            }
            if (StringUtils.isBlank(opt.getString("label"))) {
                throw new ServiceException(MessageUtils.message("defect.field.enum_options_invalid"));
            }
        }
    }

    private static void validateArrayConfig(JSONObject cfg) {
        String itemType = cfg.getString("itemType");
        if (StringUtils.isBlank(itemType) || !ARRAY_ITEM_TYPES.contains(itemType)) {
            throw new ServiceException(MessageUtils.message("defect.field.array_item_type_invalid"));
        }
        if ("enum".equals(itemType)) {
            validateEnumConfig(cfg);
        }
    }

    private void assertEnabledLimit(Long projectId, Long excludeFieldId, Integer enabledFlag) {
        if (enabledFlag == null || enabledFlag != 1) {
            return;
        }
        int count = fieldMapper.countEnabledByProjectId(projectId);
        if (excludeFieldId != null) {
            SysProjectDefectField existing = fieldMapper.selectByFieldId(excludeFieldId);
            if (existing != null && existing.getEnabled() != null && existing.getEnabled() == 1) {
                count--;
            }
        }
        if (count >= MAX_ENABLED_FIELDS) {
            throw new ServiceException(MessageUtils.message("defect.field.enabled_limit", MAX_ENABLED_FIELDS));
        }
    }

    private void assertFieldLabelLength(String fieldLabel) {
        if (StringUtils.isBlank(fieldLabel)) {
            return;
        }
        if (fieldLabel.trim().length() > MAX_FIELD_LABEL_LENGTH) {
            throw new ServiceException(MessageUtils.message("defect.field.label_too_long", MAX_FIELD_LABEL_LENGTH));
        }
    }

    private void assertFieldLabelUnique(Long projectId, Long excludeFieldId, String fieldLabel) {
        if (StringUtils.isBlank(fieldLabel)) {
            return;
        }
        String normalized = fieldLabel.trim().toLowerCase();
        for (BuiltinDef def : DefectBuiltinFieldRegistry.definitions()) {
            String builtinLabel = resolveBuiltinLabel(def.fieldKey).trim().toLowerCase();
            if (builtinLabel.equals(normalized)) {
                throw new ServiceException(MessageUtils.message("defect.field.label_duplicate"));
            }
        }
        SysProjectDefectField existing = fieldMapper.selectByProjectIdAndFieldLabel(projectId, fieldLabel);
        if (existing != null && (excludeFieldId == null || !existing.getFieldId().equals(excludeFieldId))) {
            throw new ServiceException(MessageUtils.message("defect.field.label_duplicate"));
        }
    }
}
