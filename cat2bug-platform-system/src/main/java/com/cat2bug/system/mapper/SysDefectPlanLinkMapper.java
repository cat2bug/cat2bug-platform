package com.cat2bug.system.mapper;

import com.cat2bug.common.domain.SysDefectPlanLink;
import com.cat2bug.system.mybatis.QuarkusMapperDataSources;
import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface SysDefectPlanLinkMapper {

    int insert(SysDefectPlanLink row);

    int deleteByLinkId(@Param("linkId") Long linkId);

    SysDefectPlanLink selectByLinkId(@Param("linkId") Long linkId);

    List<SysDefectPlanLink> selectByProjectId(@Param("projectId") Long projectId);

    List<SysDefectPlanLink> selectByProjectAndDefectIds(@Param("projectId") Long projectId,
                                                       @Param("defectIds") List<Long> defectIds);
}
