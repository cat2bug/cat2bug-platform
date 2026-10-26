package com.cat2bug.system.mapper;

import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import com.cat2bug.system.mybatis.QuarkusMapperDataSources;
import com.cat2bug.common.domain.SysPlan;
import com.cat2bug.common.domain.dashboard.SysAction;
import com.cat2bug.common.domain.dashboard.SysCaseStatistics;
import com.cat2bug.common.domain.dashboard.SysDefectLine;
import com.cat2bug.common.domain.dashboard.SysDefectStatistics;
import com.cat2bug.common.domain.dashboard.SysDocumentStatistics;
import com.cat2bug.common.domain.dashboard.SysMemberStatistics;
import com.cat2bug.common.domain.dashboard.SysColumnsInChart;
import com.cat2bug.common.domain.dashboard.SysMemberOfDefectsLine;
import com.cat2bug.common.domain.dashboard.SysMemberRankOfDefects;
import com.cat2bug.common.domain.dashboard.SysModuleStatistics;
import com.cat2bug.common.domain.dashboard.SysWeekOpsStatistics;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface SysDashboardMapper {

    SysCaseStatistics caseStatistics(@Param("projectId") Long projectId);

    SysDefectStatistics defectStatistics(@Param("projectId") Long projectId);

    SysModuleStatistics moduleStatistics(@Param("projectId") Long projectId);

    SysDocumentStatistics documentStatistics(@Param("projectId") Long projectId);

    SysMemberStatistics memberStatistics(@Param("projectId") Long projectId);

    List<SysDefectLine> defectLine(@Param("projectId") Long projectId, @Param("timeType") String timeType);

    List<SysAction> actonList(@Param("projectId") Long projectId, @Param("type") String type);

    List<SysPlan> planList(@Param("projectId") Long projectId);

    SysPlan selectPlanByPlanId(@Param("planId") String planId);

    List<SysColumnsInChart> planBurndown(@Param("planId") String planId);

    List<SysMemberRankOfDefects> memberRankOfDefects(@Param("projectId") Long projectId);

    List<SysMemberOfDefectsLine> memberOfDefectsLine(@Param("projectId") Long projectId,
                                                     @Param("timeType") String timeType);

    /**
     * 本周：测试提/关、开发修、外部提（按缺陷日志 × 当前项目角色）。
     */
    SysWeekOpsStatistics weekRoleDefectOps(@Param("projectId") Long projectId,
                                           @Param("weekStart") Date weekStart,
                                           @Param("weekEnd") Date weekEnd);

    /**
     * 本周：MCP / API / LLM 调用次数（操作日志渠道；暂无项目维度）。
     */
    SysWeekOpsStatistics weekChannelOps(@Param("weekStart") Date weekStart,
                                        @Param("weekEnd") Date weekEnd);
}
