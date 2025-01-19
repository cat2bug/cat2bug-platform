package com.cat2bug.system.service.impl;

import com.cat2bug.system.domain.SysDefectLine;
import com.cat2bug.system.mapper.SysDashboardMapper;
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
}
