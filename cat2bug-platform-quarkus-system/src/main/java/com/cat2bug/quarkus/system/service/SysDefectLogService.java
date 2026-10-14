package com.cat2bug.quarkus.system.service;

import com.cat2bug.quarkus.common.api.TableDataInfo;
import com.cat2bug.quarkus.common.core.page.PageDomain;
import com.cat2bug.quarkus.common.domain.SysDefectLog;
import com.cat2bug.quarkus.system.mapper.SysDefectLogMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Date;
import java.util.List;

@ApplicationScoped
public class SysDefectLogService {

    @Inject
    SysDefectLogMapper sysDefectLogMapper;

    public TableDataInfo selectLogPage(SysDefectLog query, Integer pageNum, Integer pageSize) {
        PageDomain page = PageDomain.of(pageNum, pageSize);
        long total = sysDefectLogMapper.countSysDefectLogList(query);
        List<SysDefectLog> rows = sysDefectLogMapper.selectSysDefectLogList(query, page.getOffset(), page.getPageSize());
        return new TableDataInfo(rows, total);
    }

    public SysDefectLog selectSysDefectLogByDefectLogId(Long defectLogId) {
        return sysDefectLogMapper.selectSysDefectLogByDefectLogId(defectLogId);
    }

    public List<SysDefectLog> selectSysDefectLogList(SysDefectLog query) {
        return sysDefectLogMapper.selectSysDefectLogList(query, 0, Integer.MAX_VALUE);
    }

    public int insertSysDefectLog(SysDefectLog sysDefectLog) {
        sysDefectLog.setCreateTime(new Date());
        return sysDefectLogMapper.insertSysDefectLog(sysDefectLog);
    }

    public int updateSysDefectLog(SysDefectLog sysDefectLog) {
        return sysDefectLogMapper.updateSysDefectLog(sysDefectLog);
    }

    public int deleteSysDefectLogByDefectLogIds(Long[] defectLogIds) {
        if (defectLogIds == null || defectLogIds.length == 0) {
            return 0;
        }
        return sysDefectLogMapper.deleteSysDefectLogByDefectLogIds(defectLogIds);
    }
}
