package com.cat2bug.system.mapper;

import com.cat2bug.system.domain.SysReportHistory;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 报告编辑历史 Mapper
 */
public interface SysReportHistoryMapper {
    /**
     * 按报告查询历史列表（不含正文大字段时可按需扩展；当前含快照供回看）
     */
    List<SysReportHistory> selectSysReportHistoryListByReportId(@Param("reportId") Long reportId);

    /**
     * 查询单条历史
     */
    SysReportHistory selectSysReportHistoryByHistoryId(@Param("historyId") Long historyId);

    /**
     * 新增历史
     */
    int insertSysReportHistory(SysReportHistory history);

    /**
     * 按历史 ID 删除单条
     */
    int deleteSysReportHistoryByHistoryId(@Param("historyId") Long historyId);

    /**
     * 按报告删除全部历史
     */
    int deleteSysReportHistoryByReportId(@Param("reportId") Long reportId);

    /**
     * 按报告批量删除历史
     */
    int deleteSysReportHistoryByReportIds(@Param("reportIds") Long[] reportIds);
}
