package com.cat2bug.system.mapper;

import com.cat2bug.system.domain.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysDashboardMapper {
    /**
     * 测试用例统计
     * @param project
     * @return
     */
    public SysCaseStatistics caseStatistics(Long project);

    public SysDefectStatistics defectStatistics(Long project);

    public SysModuleStatistics moduleStatistics(Long project);

    public SysReportStatistics reportStatistics(Long project);

    public SysDocumentStatistics documentStatistics(Long projectId);

    public SysMemberStatistics memberStatistics(Long projectId);

    public List<SysDefectLine> defectLine(@Param("projectId") Long projectId, @Param("timeType") String timeType);

    public List<SysAction> actonList(@Param("projectId") Long projectId, @Param("type") String type);

    public List<SysColumnsInChart> planBurndown(@Param("planId") String planId);

    public List<SysMemberRankOfDefects> memberRankOfDefects(@Param("projectId") Long projectId);
}
