package com.cat2bug.system.service;

import com.cat2bug.common.domain.SysProjectDefectField;
import com.cat2bug.common.domain.SysProjectDefectFieldColumnLayout;
import com.cat2bug.common.domain.SysProjectDefectFieldManageItem;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.common.util.StringUtils;
import com.cat2bug.system.mapper.SysProjectDefectFieldMapper;
import com.cat2bug.system.util.DefectBuiltinFieldRegistry;
import com.cat2bug.system.util.DefectBuiltinFieldRegistry.BuiltinDef;
import com.cat2bug.system.util.DefectCustomFieldsValidator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@ApplicationScoped
public class SysProjectDefectFieldService {

    public static final int MAX_ENABLED_FIELDS = 50;
    public static final int MAX_FIELD_LABEL_LENGTH = 6;
    public static final Pattern FIELD_KEY_PATTERN = Pattern.compile("^[a-z][a-z0-9_]{0,63}$");

    private static final Set<String> FIELD_TYPES = new HashSet<>(Arrays.asList(
            "string", "number", "boolean", "datetime", "enum", "object", "image", "file", "array"
    ));

    private static final Set<String> ARRAY_ITEM_TYPES = new HashSet<>(Arrays.asList(
            "string", "number", "boolean", "datetime", "enum", "object", "image", "file"
    ));

    private static final Set<String> NO_DEFAULT_VALUE_TYPES = new HashSet<>(Arrays.asList(
            "image", "file"
    ));

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Inject
    SysProjectDefectFieldMapper fieldMapper;

    @Inject
    DefectCustomFieldsValidator customFieldsValidator;

    public Long resolveTeamIdFromProject(Long projectId) {
        if (projectId == null) {
            throw new ApiException("项目ID不能为空");
        }
        Long teamId = fieldMapper.selectTeamIdByProjectId(projectId);
        if (teamId == null) {
            throw new ApiException("项目不存在");
        }
        return teamId;
    }

    public List<SysProjectDefectField> selectListByProjectId(Long projectId) {
        return selectListByTeamId(resolveTeamIdFromProject(projectId));
    }

    public List<SysProjectDefectField> selectListByTeamId(Long teamId) {
        return fieldMapper.selectListByTeamId(teamId);
    }

    public List<SysProjectDefectFieldManageItem> selectManageListByProjectId(Long projectId) {
        return selectManageListByTeamId(resolveTeamIdFromProject(projectId));
    }

    public List<SysProjectDefectFieldManageItem> selectManageListByTeamId(Long teamId) {
        Map<String, BuiltinDef> defs = DefectBuiltinFieldRegistry.definitionByKey();
        ObjectNode config = loadBuiltinConfigObject(teamId);
        List<SysProjectDefectFieldManageItem> items = new ArrayList<>();

        for (BuiltinDef def : DefectBuiltinFieldRegistry.definitions()) {
            JsonNode entry = config.get(def.fieldKey);
            int enabled = entry != null && entry.has("enabled") ? entry.get("enabled").asInt() : 1;
            if (DefectBuiltinFieldRegistry.isAlwaysEnabled(def.fieldKey)) {
                enabled = 1;
            }
            int sortOrder = entry != null && entry.has("sortOrder")
                    ? entry.get("sortOrder").asInt() : def.defaultSort;
            items.add(DefectBuiltinFieldRegistry.toManageItem(
                    def, DefectBuiltinFieldRegistry.resolveBuiltinLabel(def.fieldKey), enabled, sortOrder));
        }

        for (SysProjectDefectField field : fieldMapper.selectListByTeamId(teamId)) {
            items.add(toCustomManageItem(field));
        }

        items.sort(Comparator
                .comparing(SysProjectDefectFieldManageItem::getSortOrder,
                        Comparator.nullsLast(Integer::compareTo))
                .thenComparing(item -> item.getFieldKey() != null ? item.getFieldKey() : "",
                        Comparator.nullsLast(String::compareTo)));
        return items;
    }

    @Transactional
    public int updateBuiltinFieldLayout(Long teamId, List<SysProjectDefectFieldManageItem> layout) {
        if (layout == null || layout.isEmpty()) {
            return 0;
        }
        Map<String, BuiltinDef> defs = DefectBuiltinFieldRegistry.definitionByKey();
        ObjectNode config = loadBuiltinConfigObject(teamId);
        for (SysProjectDefectFieldManageItem item : layout) {
            if (item == null || StringUtils.isEmpty(item.getFieldKey()) || !defs.containsKey(item.getFieldKey())) {
                continue;
            }
            ObjectNode entry = config.has(item.getFieldKey())
                    ? (ObjectNode) config.get(item.getFieldKey())
                    : config.putObject(item.getFieldKey());
            if (item.getEnabled() != null) {
                int enabled = DefectBuiltinFieldRegistry.isAlwaysEnabled(item.getFieldKey()) ? 1 : item.getEnabled();
                entry.put("enabled", enabled);
            }
            if (item.getSortOrder() != null) {
                entry.put("sortOrder", item.getSortOrder());
            }
        }
        try {
            return fieldMapper.updateDefectBuiltinFieldConfig(
                    teamId, MAPPER.writeValueAsString(config), new Date());
        } catch (Exception e) {
            throw new ApiException("更新内置字段布局失败");
        }
    }

    public List<SysProjectDefectField> selectEnabledListByProjectId(Long projectId) {
        return selectEnabledListByTeamId(resolveTeamIdFromProject(projectId));
    }

    public List<SysProjectDefectField> selectEnabledListByTeamId(Long teamId) {
        return fieldMapper.selectEnabledListByTeamId(teamId);
    }

    public SysProjectDefectFieldColumnLayout selectColumnLayoutByProjectId(Long projectId) {
        return selectColumnLayoutByTeamId(resolveTeamIdFromProject(projectId));
    }

    public SysProjectDefectFieldColumnLayout selectColumnLayoutByTeamId(Long teamId) {
        ObjectNode config = loadBuiltinConfigObject(teamId);
        List<String> enabledBuiltinKeys = new ArrayList<>();
        for (BuiltinDef def : DefectBuiltinFieldRegistry.definitions()) {
            if (DefectBuiltinFieldRegistry.isAlwaysEnabled(def.fieldKey)) {
                enabledBuiltinKeys.add(def.fieldKey);
                continue;
            }
            JsonNode entry = config.get(def.fieldKey);
            if (entry != null && entry.has("enabled") && entry.get("enabled").asInt() == 1) {
                enabledBuiltinKeys.add(def.fieldKey);
            }
        }
        SysProjectDefectFieldColumnLayout layout = new SysProjectDefectFieldColumnLayout();
        layout.setEnabledBuiltinFieldKeys(enabledBuiltinKeys);
        layout.setCustomFields(fieldMapper.selectEnabledListByTeamId(teamId));
        List<String> orderedEnabledFieldKeys = new ArrayList<>();
        for (SysProjectDefectFieldManageItem item : selectManageListByTeamId(teamId)) {
            if (item.getEnabled() != null && item.getEnabled() == 1 && StringUtils.isNotEmpty(item.getFieldKey())) {
                orderedEnabledFieldKeys.add(item.getFieldKey());
            }
        }
        layout.setOrderedEnabledFieldKeys(orderedEnabledFieldKeys);
        return layout;
    }

    public SysProjectDefectField selectByFieldId(Long fieldId) {
        return fieldMapper.selectByFieldId(fieldId);
    }

    @Transactional
    public SysProjectDefectField insert(SysProjectDefectField field) {
        if (field.getTeamId() == null) {
            throw new ApiException("团队ID不能为空");
        }
        normalizeDefaults(field);
        validateFieldKey(field.getFieldKey());
        validateFieldType(field.getFieldType());
        validateTypeConfig(field.getFieldType(), field.getTypeConfig());
        normalizeDefaultValue(field);

        if (fieldMapper.selectByTeamIdAndFieldKey(field.getTeamId(), field.getFieldKey()) != null) {
            throw new ApiException("属性键在该团队中已存在");
        }
        if (DefectBuiltinFieldRegistry.definitionByKey().containsKey(field.getFieldKey())) {
            throw new ApiException("属性键与缺陷内置属性冲突，请更换");
        }
        assertFieldLabelLength(field.getFieldLabel());
        assertFieldLabelUnique(field.getTeamId(), null, field.getFieldLabel());
        assertEnabledLimit(field.getTeamId(), null, field.getEnabled());

        Date now = new Date();
        field.setCreateTime(now);
        field.setUpdateTime(now);
        if (fieldMapper.insert(field) != 1) {
            throw new ApiException("创建字段定义失败");
        }
        return field;
    }

    @Transactional
    public int update(SysProjectDefectField field) {
        if (field.getFieldId() == null) {
            throw new ApiException("字段ID不能为空");
        }
        SysProjectDefectField existing = fieldMapper.selectByFieldId(field.getFieldId());
        if (existing == null || !existing.getTeamId().equals(field.getTeamId())) {
            throw new ApiException("字段定义不存在");
        }
        if (field.getFieldKey() != null && !field.getFieldKey().equals(existing.getFieldKey())) {
            throw new ApiException("属性键创建后不可修改");
        }

        String newType = field.getFieldType() != null ? field.getFieldType() : existing.getFieldType();
        if (field.getFieldType() != null && !field.getFieldType().equals(existing.getFieldType())) {
            if (fieldMapper.countCustomFieldKeyUsedByTeamId(existing.getTeamId(), existing.getFieldKey()) > 0) {
                throw new ApiException("该字段已有缺陷数据，不可修改类型");
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
        assertEnabledLimit(existing.getTeamId(), existing.getFieldId(), newEnabled);

        String newLabel = field.getFieldLabel() != null ? field.getFieldLabel() : existing.getFieldLabel();
        assertFieldLabelLength(newLabel);
        assertFieldLabelUnique(existing.getTeamId(), existing.getFieldId(), newLabel);

        field.setFieldKey(existing.getFieldKey());
        field.setTeamId(existing.getTeamId());
        field.setUpdateTime(new Date());
        return fieldMapper.update(field);
    }

    @Transactional
    public int softDelete(Long teamId, Long fieldId) {
        SysProjectDefectField existing = fieldMapper.selectByFieldId(fieldId);
        if (existing == null || !existing.getTeamId().equals(teamId)) {
            throw new ApiException("字段定义不存在");
        }
        return fieldMapper.softDelete(fieldId, teamId, new Date());
    }

    private ObjectNode loadBuiltinConfigObject(Long teamId) {
        String raw = fieldMapper.selectDefectBuiltinFieldConfig(teamId);
        ObjectNode stored;
        try {
            stored = (raw != null && !raw.isBlank())
                    ? (ObjectNode) MAPPER.readTree(raw)
                    : MAPPER.createObjectNode();
        } catch (Exception e) {
            stored = MAPPER.createObjectNode();
        }
        ObjectNode merged = MAPPER.createObjectNode();
        for (BuiltinDef def : DefectBuiltinFieldRegistry.definitions()) {
            JsonNode entry = stored.get(def.fieldKey);
            int enabled = 1;
            int sortOrder = def.defaultSort;
            if (entry != null) {
                if (entry.has("enabled")) {
                    enabled = entry.get("enabled").asInt();
                }
                if (entry.has("sortOrder")) {
                    sortOrder = entry.get("sortOrder").asInt();
                }
            }
            if (DefectBuiltinFieldRegistry.isAlwaysEnabled(def.fieldKey)) {
                enabled = 1;
            }
            ObjectNode normalized = MAPPER.createObjectNode();
            normalized.put("enabled", enabled);
            normalized.put("sortOrder", sortOrder);
            merged.set(def.fieldKey, normalized);
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
            field.setTypeConfig(new LinkedHashMap<>());
        }
    }

    public static void validateFieldKey(String fieldKey) {
        if (StringUtils.isEmpty(fieldKey) || !FIELD_KEY_PATTERN.matcher(fieldKey).matches()) {
            throw new ApiException("属性键格式无效，须以小写字母开头且仅含小写字母、数字、下划线");
        }
    }

    public static void validateFieldType(String fieldType) {
        if (StringUtils.isEmpty(fieldType) || !FIELD_TYPES.contains(fieldType)) {
            throw new ApiException("字段类型无效");
        }
    }

    public static void validateTypeConfig(String fieldType, Map<String, Object> typeConfig) {
        Map<String, Object> cfg = typeConfig != null ? typeConfig : new LinkedHashMap<>();
        if ("enum".equals(fieldType)) {
            validateEnumConfig(cfg);
        } else if ("array".equals(fieldType)) {
            validateArrayConfig(cfg);
        }
    }

    @SuppressWarnings("unchecked")
    private static void validateEnumConfig(Map<String, Object> cfg) {
        Object optionsObj = cfg.get("options");
        if (!(optionsObj instanceof List) || ((List<?>) optionsObj).isEmpty()) {
            throw new ApiException("枚举类型须配置 options");
        }
        Set<String> keys = new HashSet<>();
        for (Object optObj : (List<?>) optionsObj) {
            if (!(optObj instanceof Map)) {
                throw new ApiException("枚举 options 配置无效（须含非重复 key 与 label）");
            }
            Map<String, Object> opt = (Map<String, Object>) optObj;
            Object key = opt.get("key");
            if (key == null || String.valueOf(key).isBlank() || !keys.add(String.valueOf(key))) {
                throw new ApiException("枚举 options 配置无效（须含非重复 key 与 label）");
            }
            Object label = opt.get("label");
            if (label == null || String.valueOf(label).isBlank()) {
                throw new ApiException("枚举 options 配置无效（须含非重复 key 与 label）");
            }
        }
    }

    private static void validateArrayConfig(Map<String, Object> cfg) {
        Object itemTypeObj = cfg.get("itemType");
        String itemType = itemTypeObj != null ? String.valueOf(itemTypeObj) : null;
        if (StringUtils.isEmpty(itemType) || !ARRAY_ITEM_TYPES.contains(itemType)) {
            throw new ApiException("数组类型须配置有效 itemType");
        }
        if ("enum".equals(itemType)) {
            validateEnumConfig(cfg);
        }
    }

    private void assertEnabledLimit(Long teamId, Long excludeFieldId, Integer enabledFlag) {
        if (enabledFlag == null || enabledFlag != 1) {
            return;
        }
        int count = fieldMapper.countEnabledByTeamId(teamId);
        if (excludeFieldId != null) {
            SysProjectDefectField existing = fieldMapper.selectByFieldId(excludeFieldId);
            if (existing != null && existing.getEnabled() != null && existing.getEnabled() == 1) {
                count--;
            }
        }
        if (count >= MAX_ENABLED_FIELDS) {
            throw new ApiException("单团队启用字段不得超过 " + MAX_ENABLED_FIELDS + " 个");
        }
    }

    private void assertFieldLabelLength(String fieldLabel) {
        if (StringUtils.isEmpty(fieldLabel)) {
            return;
        }
        if (fieldLabel.trim().length() > MAX_FIELD_LABEL_LENGTH) {
            throw new ApiException("显示名称不能超过 " + MAX_FIELD_LABEL_LENGTH + " 个字");
        }
    }

    private void assertFieldLabelUnique(Long teamId, Long excludeFieldId, String fieldLabel) {
        if (StringUtils.isEmpty(fieldLabel)) {
            return;
        }
        String normalized = fieldLabel.trim().toLowerCase();
        for (BuiltinDef def : DefectBuiltinFieldRegistry.definitions()) {
            String builtinLabel = DefectBuiltinFieldRegistry.resolveBuiltinLabel(def.fieldKey).trim().toLowerCase();
            if (builtinLabel.equals(normalized)) {
                throw new ApiException("显示名称在该团队中已存在");
            }
        }
        SysProjectDefectField existing = fieldMapper.selectByTeamIdAndFieldLabel(teamId, fieldLabel);
        if (existing != null && (excludeFieldId == null || !existing.getFieldId().equals(excludeFieldId))) {
            throw new ApiException("显示名称在该团队中已存在");
        }
    }
}
