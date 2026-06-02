package com.cat2bug.system.service;

import com.cat2bug.system.domain.SysProjectDefectField;
import com.cat2bug.system.domain.SysProjectDefectFieldColumnLayout;
import com.cat2bug.system.domain.SysProjectDefectFieldManageItem;

import java.util.List;

/**
 * 项目缺陷自定义字段定义 Service
 */
public interface ISysProjectDefectFieldService {

    List<SysProjectDefectField> selectListByProjectId(Long projectId);

    /** 管理页：内置属性 + 自定义字段 */
    List<SysProjectDefectFieldManageItem> selectManageListByProjectId(Long projectId);

    /** 更新内置属性的启用与排序 */
    int updateBuiltinFieldLayout(Long projectId, List<SysProjectDefectFieldManageItem> layout);

    List<SysProjectDefectField> selectEnabledListByProjectId(Long projectId);

    /** 列表/表单：启用的内置字段键 + 启用的自定义字段 */
    SysProjectDefectFieldColumnLayout selectColumnLayoutByProjectId(Long projectId);

    SysProjectDefectField selectByFieldId(Long fieldId);

    SysProjectDefectField insert(SysProjectDefectField field);

    int update(SysProjectDefectField field);

    int softDelete(Long projectId, Long fieldId);
}
