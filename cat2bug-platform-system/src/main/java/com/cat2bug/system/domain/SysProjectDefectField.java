package com.cat2bug.system.domain;

import com.cat2bug.common.annotation.Excel;
import com.cat2bug.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * 项目缺陷自定义字段定义 sys_project_defect_field
 */
@EqualsAndHashCode(callSuper = false)
@Data
public class SysProjectDefectField extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 字段ID */
    private Long fieldId;

    /** 项目ID */
    @Excel(name = "项目ID")
    private Long projectId;

    /** 字段键（项目内唯一，创建后不可改） */
    @Excel(name = "字段键")
    private String fieldKey;

    /** 显示名称 */
    @Excel(name = "显示名称")
    private String fieldLabel;

    /** 字段类型 */
    @Excel(name = "字段类型")
    private String fieldType;

    /** 字符串最大长度 */
    private Integer maxLength;

    /** 是否必填 */
    private Integer required;

    /** 是否可空 */
    private Integer nullable;

    /** 是否启用 */
    private Integer enabled;

    /** 是否系统预置字段（1=系统，不可删除） */
    private Integer isSystem;

    /** 排序 */
    private Integer sortOrder;

    /** 类型扩展配置 */
    private Map<String, Object> typeConfig;

    /** 新建缺陷时的默认值（JSON，形态与 custom_fields 中单键值一致） */
    private Object defaultValue;
}
