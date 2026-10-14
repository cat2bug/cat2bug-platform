package com.cat2bug.quarkus.system.mapper;

import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import com.cat2bug.quarkus.system.mybatis.QuarkusMapperDataSources;
import com.cat2bug.quarkus.common.domain.SysDefect;
import com.cat2bug.quarkus.common.domain.SysPlan;
import com.cat2bug.quarkus.common.domain.SysPlanItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface SysPlanMapper {

    long countSysPlanList(@Param("query") SysPlan query);

    List<SysPlan> selectSysPlanList(@Param("query") SysPlan query,
                                    @Param("offset") int offset,
                                    @Param("limit") int limit);

    SysPlan selectSysPlanByPlanId(@Param("planId") String planId);

    long getProjectPlanMaxNum(@Param("projectId") Long projectId);

    List<SysPlanItem> selectSysPlanItemListByPlanId(@Param("planId") String planId);

    int insertSysPlan(SysPlan sysPlan);

    int updateSysPlan(SysPlan sysPlan);

    int updateUpdateTimeOfSysPlan(SysPlan sysPlan);

    int deleteSysPlanByPlanId(@Param("planId") String planId);

    int deleteSysPlanByPlanIds(@Param("array") String[] planIds);

    int deleteSysPlanItemByPlanIds(@Param("array") String[] planIds);

    int deleteSysPlanItemByPlanId(@Param("planId") String planId);

    int deleteSysPlanItemByPlanItemIds(@Param("array") String[] planItemIds);

    int batchSysPlanItem(@Param("list") List<SysPlanItem> list);

    long countSysDefectListByPlanId(@Param("planId") String planId, @Param("defect") SysDefect defect);

    List<SysDefect> selectSysDefectListByPlanId(@Param("planId") String planId,
                                                @Param("defect") SysDefect defect,
                                                @Param("offset") int offset,
                                                @Param("limit") int limit);
}
