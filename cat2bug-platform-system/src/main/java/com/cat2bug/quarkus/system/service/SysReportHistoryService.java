package com.cat2bug.quarkus.system.service;

import com.cat2bug.quarkus.common.domain.SysReport;
import com.cat2bug.quarkus.common.domain.SysReportHistory;
import com.cat2bug.quarkus.system.mapper.SysReportHistoryMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.Collections;
import java.util.List;

@ApplicationScoped
public class SysReportHistoryService {

    @Inject
    SysReportHistoryMapper sysReportHistoryMapper;

    @Inject
    SysReportService sysReportService;

    public List<SysReportHistory> selectSysReportHistoryListByReportId(Long reportId) {
        if (reportId == null) {
            return Collections.emptyList();
        }
        return sysReportHistoryMapper.selectSysReportHistoryListByReportId(reportId);
    }

    public SysReportHistory selectSysReportHistoryByHistoryId(Long historyId) {
        if (historyId == null) {
            return null;
        }
        return sysReportHistoryMapper.selectSysReportHistoryByHistoryId(historyId);
    }

    public int insertSysReportHistory(SysReportHistory history) {
        return sysReportHistoryMapper.insertSysReportHistory(history);
    }

    public int deleteSysReportHistoryByHistoryId(Long historyId) {
        if (historyId == null) {
            return 0;
        }
        return sysReportHistoryMapper.deleteSysReportHistoryByHistoryId(historyId);
    }

    /** 恢复同样走正文保存流程，正文有变化时会再写入一条历史。 */
    @Transactional
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
