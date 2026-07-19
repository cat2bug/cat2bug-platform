package com.cat2bug.system.service.impl;

import com.cat2bug.common.core.domain.entity.SysReport;
import com.cat2bug.system.domain.SysReportHistory;
import com.cat2bug.system.mapper.SysReportHistoryMapper;
import com.cat2bug.system.service.ISysReportHistoryService;
import com.cat2bug.system.service.ISysReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * 报告编辑历史 Service 实现
 */
@Service
public class SysReportHistoryServiceImpl implements ISysReportHistoryService {
    @Autowired
    private SysReportHistoryMapper sysReportHistoryMapper;

    @Autowired
    private ISysReportService sysReportService;

    @Override
    public List<SysReportHistory> selectSysReportHistoryListByReportId(Long reportId) {
        if (reportId == null) {
            return Collections.emptyList();
        }
        return sysReportHistoryMapper.selectSysReportHistoryListByReportId(reportId);
    }

    @Override
    public SysReportHistory selectSysReportHistoryByHistoryId(Long historyId) {
        if (historyId == null) {
            return null;
        }
        return sysReportHistoryMapper.selectSysReportHistoryByHistoryId(historyId);
    }

    @Override
    public int insertSysReportHistory(SysReportHistory history) {
        return sysReportHistoryMapper.insertSysReportHistory(history);
    }

    @Override
    public int deleteSysReportHistoryByHistoryId(Long historyId) {
        if (historyId == null) {
            return 0;
        }
        return sysReportHistoryMapper.deleteSysReportHistoryByHistoryId(historyId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int restoreSysReportHistory(Long historyId) {
        SysReportHistory history = selectSysReportHistoryByHistoryId(historyId);
        if (history == null || history.getReportId() == null) {
            return 0;
        }
        SysReport patch = new SysReport();
        patch.setReportId(history.getReportId());
        patch.setReportDescription(history.getReportDescription() == null ? "" : history.getReportDescription());
        return sysReportService.updateSysReport(patch);
    }
}
