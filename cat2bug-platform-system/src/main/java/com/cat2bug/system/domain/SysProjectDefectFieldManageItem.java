package com.cat2bug.system.domain;

import lombok.Data;

import java.util.Map;

/**
 * 缺陷字段管理页统一行（内置系统属性 + 项目自定义字段）
 */
@Data
public class SysProjectDefectFieldManageItem {

    /** 是否缺陷实体内置属性（非 custom_fields） */
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

    /** 新建缺陷默认值（仅自定义字段） */
    private Object defaultValue;

    /** 类型扩展配置（如枚举 options） */
    private Map<String, Object> typeConfig;
}
