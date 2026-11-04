package com.cat2bug.system.mapper;

import com.cat2bug.common.domain.SysTeamPlan;
import com.cat2bug.system.mybatis.QuarkusMapperDataSources;
import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface SysTeamPlanMapper {

    List<SysTeamPlan> selectAll();

    SysTeamPlan selectByPlanId(@Param("planId") Long planId);

    SysTeamPlan selectSystemDefault();

    int insert(SysTeamPlan plan);

    int update(SysTeamPlan plan);

    int countTeamsByPlanId(@Param("planId") Long planId);
}
