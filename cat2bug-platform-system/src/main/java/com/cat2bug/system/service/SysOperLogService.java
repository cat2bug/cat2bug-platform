package com.cat2bug.system.service;

import com.cat2bug.common.api.TableDataInfo;
import com.cat2bug.common.core.page.PageDomain;
import com.cat2bug.common.domain.SysOperLog;
import com.cat2bug.system.mapper.SysOperLogMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
public class SysOperLogService {

    @Inject
    SysOperLogMapper sysOperLogMapper;

    public void insertOperlog(SysOperLog operLog) {
        sysOperLogMapper.insertOperlog(operLog);
    }

    public TableDataInfo selectOperLogPage(SysOperLog query, Integer pageNum, Integer pageSize) {
        PageDomain page = PageDomain.of(pageNum, pageSize);
        long total = sysOperLogMapper.countOperLogList(query);
        List<SysOperLog> rows = sysOperLogMapper.selectOperLogList(query, page.getOffset(), page.getPageSize());
        return new TableDataInfo(rows, total);
    }

    public List<SysOperLog> selectOperLogListAll(SysOperLog query) {
        List<SysOperLog> list = sysOperLogMapper.selectOperLogListAll(query);
        return list != null ? list : List.of();
    }

    public SysOperLog selectOperLogById(Long operId) {
        return sysOperLogMapper.selectOperLogById(operId);
    }

    @Transactional
    public int deleteOperLogByIds(Long[] operIds) {
        return sysOperLogMapper.deleteOperLogByIds(operIds);
    }

    @Transactional
    public void cleanOperLog() {
        sysOperLogMapper.cleanOperLog();
    }
}
