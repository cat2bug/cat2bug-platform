package com.cat2bug.system.mapper;

import com.cat2bug.system.domain.SysProjectDefectField;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 项目缺陷自定义字段定义 Mapper
 */
public interface SysProjectDefectFieldMapper {

    SysProjectDefectField selectByFieldId(@Param("fieldId") Long fieldId);

    SysProjectDefectField selectByProjectIdAndFieldKey(
            @Param("projectId") Long projectId, @Param("fieldKey") String fieldKey);

    SysProjectDefectField selectByProjectIdAndFieldLabel(
            @Param("projectId") Long projectId, @Param("fieldLabel") String fieldLabel);

    List<SysProjectDefectField> selectListByProjectId(@Param("projectId") Long projectId);

    List<SysProjectDefectField> selectEnabledListByProjectId(@Param("projectId") Long projectId);

    int countEnabledByProjectId(@Param("projectId") Long projectId);

    int countCustomFieldKeyUsed(
            @Param("projectId") Long projectId, @Param("fieldKey") String fieldKey);

    int insert(SysProjectDefectField field);

    int update(SysProjectDefectField field);

    int softDelete(
            @Param("fieldId") Long fieldId,
            @Param("projectId") Long projectId,
            @Param("updateTime") java.util.Date updateTime);

    String selectDefectBuiltinFieldConfig(@Param("projectId") Long projectId);

    int updateDefectBuiltinFieldConfig(
            @Param("projectId") Long projectId,
            @Param("config") String config,
            @Param("updateTime") java.util.Date updateTime);
}
