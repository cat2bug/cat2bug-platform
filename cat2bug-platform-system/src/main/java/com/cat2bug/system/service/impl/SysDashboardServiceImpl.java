package com.cat2bug.system.service.impl;

import com.cat2bug.system.domain.*;
import com.cat2bug.system.mapper.SysDashboardMapper;
import com.cat2bug.system.mapper.SysPlanMapper;
import com.cat2bug.system.service.ISysDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @Author: yuzhantao
 * @CreateTime: 2025-01-19 04:48
 * @Version: 1.0.0
 */
@Service
public class SysDashboardServiceImpl implements ISysDashboardService {
    @Autowired
    private SysDashboardMapper sysDashboardMapper;

    @Override
    public List<SysDefectLine> defectLine(Long projectId, String timeType) {
        return sysDashboardMapper.defectLine(projectId, timeType);
    }

    @Override
    public List<SysColumnsInChart> planBurndown(String planId) {
        return sysDashboardMapper.planBurndown(planId);
    }

    @Override
    public List<SysMemberRankOfDefects> memberRankOfDefects(Long projectId) {
        return sysDashboardMapper.memberRankOfDefects(projectId);
    }

    @Override
    public List<SysMemberOfDefectsLine> memberOfDefectsLine(Long projectId, String timeType) {
        return sysDashboardMapper.memberOfDefectsLine(projectId, timeType);
    }

    @Override
    public SysCaseStatistics caseStatistics(Long projectId) {
        return sysDashboardMapper.caseStatistics(projectId);
    }

    @Override
    public SysDefectStatistics defectStatistics(Long projectId) {
        return sysDashboardMapper.defectStatistics(projectId);
    }

    @Override
    public SysModuleStatistics moduleStatistics(Long projectId) {
        return sysDashboardMapper.moduleStatistics(projectId);
    }

    @Override
    public SysReportStatistics reportStatistics(Long projectId) {
        return sysDashboardMapper.reportStatistics(projectId);
    }

    @Override
    public SysDocumentStatistics documentStatistics(Long projectId) {
        return sysDashboardMapper.documentStatistics(projectId);
    }

    @Override
    public SysMemberStatistics memberStatistics(Long projectId) {
        return sysDashboardMapper.memberStatistics(projectId);
    }

    @Override
    public List<SysAction> actonList(Long projectId, String type) {
        return sysDashboardMapper.actonList(projectId, type);
    }
}
