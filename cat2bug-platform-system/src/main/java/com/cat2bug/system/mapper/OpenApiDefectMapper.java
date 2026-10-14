package com.cat2bug.system.mapper;

import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import com.cat2bug.system.mybatis.QuarkusMapperDataSources;
import com.cat2bug.common.domain.open.ApiDefect;
import com.cat2bug.common.domain.open.ApiDefectRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface OpenApiDefectMapper {

    Long getProjectDefectMaxNum(@Param("projectId") Long projectId);

    ApiDefect selectSysDefectByDefectNumber(@Param("projectId") Long projectId, @Param("defectNum") Long defectNumber);

    List<ApiDefect> selectApiDefectList(@Param("projectId") Long projectId, @Param("defect") ApiDefectRequest apiDefect);

    long countApiDefectList(@Param("projectId") Long projectId, @Param("defect") ApiDefectRequest apiDefect);

    List<ApiDefect> selectApiDefectPage(@Param("projectId") Long projectId,
                                        @Param("defect") ApiDefectRequest apiDefect,
                                        @Param("offset") int offset,
                                        @Param("limit") int limit);

    List<ApiDefect> selectApiDefectListByDefectName(@Param("projectId") Long projectId, @Param("defectName") String defectName);

    int insertApiDefect(@Param("projectId") Long projectId, @Param("defect") ApiDefectRequest apiDefect);

    int batchInsertSysDefect(@Param("projectId") Long projectId, @Param("defectList") List<ApiDefectRequest> defectList);

    List<Long> selectDefectIdsByKey(@Param("projectId") Long projectId,
                                    @Param("defectGroupKey") String defectGroupKey,
                                    @Param("defectKey") String defectKey);

    int updateApiDefect(ApiDefectRequest apiDefectRequest);
}
