package com.cat2bug.system.mapper;

import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import com.cat2bug.system.mybatis.QuarkusMapperDataSources;
import com.cat2bug.common.domain.SysDefectOpenWorkload;
import com.cat2bug.common.domain.SysDefectOpenWorkloadSummary;
import com.cat2bug.common.domain.SysDefectParticipationDay;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface SysDefectStatisticMapper {

    List<Map<String, Object>> typeStatistic(@Param("projectId") Long projectId,
                                            @Param("memberId") Long memberId);

    List<Map<String, Object>> stateStatistic(@Param("projectId") Long projectId,
                                             @Param("memberId") Long memberId);

    List<Map<String, Object>> moduleStatistic(@Param("projectId") Long projectId);

    List<SysDefectOpenWorkload> openWorkloadByProject(@Param("projectId") Long projectId);

    SysDefectOpenWorkloadSummary openWorkloadByMember(@Param("projectId") Long projectId,
                                                    @Param("userId") Long userId);

    List<SysDefectParticipationDay> participationByDay(@Param("projectId") Long projectId,
                                                       @Param("userId") Long userId,
                                                       @Param("startDate") String startDate);
}
