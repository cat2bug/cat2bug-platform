package com.cat2bug.system.mapper;

import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import com.cat2bug.system.mybatis.QuarkusMapperDataSources;
import com.cat2bug.common.domain.SysProjectDefectField;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface SysProjectDefectFieldMapper {

    Long selectTeamIdByProjectId(@Param("projectId") Long projectId);

    SysProjectDefectField selectByFieldId(@Param("fieldId") Long fieldId);

    SysProjectDefectField selectByTeamIdAndFieldKey(
            @Param("teamId") Long teamId, @Param("fieldKey") String fieldKey);

    SysProjectDefectField selectByTeamIdAndFieldLabel(
            @Param("teamId") Long teamId, @Param("fieldLabel") String fieldLabel);

    List<SysProjectDefectField> selectListByTeamId(@Param("teamId") Long teamId);

    List<SysProjectDefectField> selectEnabledListByTeamId(@Param("teamId") Long teamId);

    int countEnabledByTeamId(@Param("teamId") Long teamId);

    int countCustomFieldKeyUsedByTeamId(
            @Param("teamId") Long teamId, @Param("fieldKey") String fieldKey);

    int insert(SysProjectDefectField field);

    int update(SysProjectDefectField field);

    int softDelete(
            @Param("fieldId") Long fieldId,
            @Param("teamId") Long teamId,
            @Param("updateTime") Date updateTime);

    String selectDefectBuiltinFieldConfig(@Param("teamId") Long teamId);

    int updateDefectBuiltinFieldConfig(
            @Param("teamId") Long teamId,
            @Param("config") String config,
            @Param("updateTime") Date updateTime);
}
