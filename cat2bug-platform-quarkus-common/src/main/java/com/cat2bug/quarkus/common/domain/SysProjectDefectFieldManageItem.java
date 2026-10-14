package com.cat2bug.quarkus.common.domain;

import java.util.Map;

/**
 * 缺陷字段管理页统一行（内置系统属性 + 项目自定义字段）
 */
public class SysProjectDefectFieldManageItem {

    private Boolean systemBuiltin;
    private Long fieldId;
    private String fieldKey;
    private String fieldLabel;
    private String fieldType;
    private Integer maxLength;
    private Integer required;
    private Integer nullable;
    private Integer enabled;
    private Integer sortOrder;
    private Object defaultValue;
    private Map<String, Object> typeConfig;

    public Boolean getSystemBuiltin() {
        return systemBuiltin;
    }

    public void setSystemBuiltin(Boolean systemBuiltin) {
        this.systemBuiltin = systemBuiltin;
    }

    public Long getFieldId() {
        return fieldId;
    }

    public void setFieldId(Long fieldId) {
        this.fieldId = fieldId;
    }

    public String getFieldKey() {
        return fieldKey;
    }

    public void setFieldKey(String fieldKey) {
        this.fieldKey = fieldKey;
    }

    public String getFieldLabel() {
        return fieldLabel;
    }

    public void setFieldLabel(String fieldLabel) {
        this.fieldLabel = fieldLabel;
    }

    public String getFieldType() {
        return fieldType;
    }

    public void setFieldType(String fieldType) {
        this.fieldType = fieldType;
    }

    public Integer getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(Integer maxLength) {
        this.maxLength = maxLength;
    }

    public Integer getRequired() {
        return required;
    }

    public void setRequired(Integer required) {
        this.required = required;
    }

    public Integer getNullable() {
        return nullable;
    }

    public void setNullable(Integer nullable) {
        this.nullable = nullable;
    }

    public Integer getEnabled() {
        return enabled;
    }

    public void setEnabled(Integer enabled) {
        this.enabled = enabled;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    public Map<String, Object> getTypeConfig() {
        return typeConfig;
    }

    public void setTypeConfig(Map<String, Object> typeConfig) {
        this.typeConfig = typeConfig;
    }
}
