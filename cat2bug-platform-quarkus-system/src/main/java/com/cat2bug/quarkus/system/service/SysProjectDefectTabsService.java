package com.cat2bug.quarkus.system.service;

import com.cat2bug.quarkus.common.api.TableDataInfo;
import com.cat2bug.quarkus.common.core.page.PageDomain;
import com.cat2bug.quarkus.common.domain.SysProjectDefectTabs;
import com.cat2bug.quarkus.system.mapper.SysProjectDefectTabsMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Date;
import java.util.List;

@ApplicationScoped
public class SysProjectDefectTabsService {

    @Inject
    SysProjectDefectTabsMapper sysProjectDefectTabsMapper;

    public TableDataInfo selectTabsPage(SysProjectDefectTabs query, Integer pageNum, Integer pageSize) {
        PageDomain page = PageDomain.of(pageNum, pageSize);
        long total = sysProjectDefectTabsMapper.countSysProjectDefectTabsList(query);
        List<SysProjectDefectTabs> rows = sysProjectDefectTabsMapper.selectSysProjectDefectTabsList(
                query, page.getOffset(), page.getPageSize());
        return new TableDataInfo(rows, total);
    }

    public List<SysProjectDefectTabs> selectSysProjectDefectTabsList(SysProjectDefectTabs query) {
        return sysProjectDefectTabsMapper.selectSysProjectDefectTabsListAll(query);
    }

    public SysProjectDefectTabs selectSysProjectDefectTabsByTabId(Long tabId) {
        return sysProjectDefectTabsMapper.selectSysProjectDefectTabsByTabId(tabId);
    }

    public SysProjectDefectTabs insertSysProjectDefectTabs(SysProjectDefectTabs tabs) {
        tabs.setCreateTime(new Date());
        int rows = sysProjectDefectTabsMapper.insertSysProjectDefectTabs(tabs);
        return rows == 1 ? tabs : null;
    }

    public int updateSysProjectDefectTabs(SysProjectDefectTabs tabs) {
        return sysProjectDefectTabsMapper.updateSysProjectDefectTabs(tabs);
    }

    public int updateSort(List<SysProjectDefectTabs> list) {
        return sysProjectDefectTabsMapper.updateSort(list);
    }

    public int deleteSysProjectDefectTabsByTabIds(Long[] tabIds) {
        return sysProjectDefectTabsMapper.deleteSysProjectDefectTabsByTabIds(tabIds);
    }
}
