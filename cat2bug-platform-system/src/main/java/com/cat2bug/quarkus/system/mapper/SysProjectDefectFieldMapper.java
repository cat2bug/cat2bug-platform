package com.cat2bug.quarkus.system.mapper;

import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import com.cat2bug.quarkus.system.mybatis.QuarkusMapperDataSources;
import com.cat2bug.quarkus.common.domain.SysProjectDefectField;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
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
            @Param("updateTime") Date updateTime);

    String selectDefectBuiltinFieldConfig(@Param("projectId") Long projectId);

    int updateDefectBuiltinFieldConfig(
            @Param("projectId") Long projectId,
            @Param("config") String config,
            @Param("updateTime") Date updateTime);
}
