package com.cat2bug.system.service;

import com.cat2bug.system.domain.SysReportHistory;

import java.util.List;

/**
 * 报告编辑历史 Service
 */
public interface ISysReportHistoryService {
    /**
     * 按报告查询历史列表
     */
    List<SysReportHistory> selectSysReportHistoryListByReportId(Long reportId);

    /**
     * 查询单条历史
     */
    SysReportHistory selectSysReportHistoryByHistoryId(Long historyId);

    /**
     * 新增历史
     */
    int insertSysReportHistory(SysReportHistory history);

    /**
     * 删除单条历史
     */
    int deleteSysReportHistoryByHistoryId(Long historyId);

    /**
     * 将历史正文快照恢复到报告当前正文
     */
    int restoreSysReportHistory(Long historyId);
}
