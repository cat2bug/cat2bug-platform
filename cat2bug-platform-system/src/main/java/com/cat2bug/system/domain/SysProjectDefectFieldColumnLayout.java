package com.cat2bug.system.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * 缺陷列表/表单使用的列布局：启用的内置字段键 + 启用的自定义字段定义。
 */
public class SysProjectDefectFieldColumnLayout {

    private List<String> enabledBuiltinFieldKeys = new ArrayList<>();

    private List<SysProjectDefectField> customFields = new ArrayList<>();

    /** 已启用字段的 fieldKey，按字段管理 sortOrder 排序（内置 + 自定义） */
    private List<String> orderedEnabledFieldKeys = new ArrayList<>();

    public List<String> getEnabledBuiltinFieldKeys() {
        return enabledBuiltinFieldKeys;
    }

    public void setEnabledBuiltinFieldKeys(List<String> enabledBuiltinFieldKeys) {
        this.enabledBuiltinFieldKeys = enabledBuiltinFieldKeys;
    }

    public List<SysProjectDefectField> getCustomFields() {
        return customFields;
    }

    public void setCustomFields(List<SysProjectDefectField> customFields) {
        this.customFields = customFields;
    }

    public List<String> getOrderedEnabledFieldKeys() {
        return orderedEnabledFieldKeys;
    }

    public void setOrderedEnabledFieldKeys(List<String> orderedEnabledFieldKeys) {
        this.orderedEnabledFieldKeys = orderedEnabledFieldKeys;
    }
}
