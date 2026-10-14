package com.cat2bug.system.service;

import com.cat2bug.common.api.TableDataInfo;
import com.cat2bug.common.core.page.PageDomain;
import com.cat2bug.common.domain.SysScreenSize;
import com.cat2bug.system.mapper.SysScreenSizeMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
public class SysScreenSizeService {

    @Inject
    SysScreenSizeMapper sysScreenSizeMapper;

    public TableDataInfo selectScreenSizePage(SysScreenSize query, Integer pageNum, Integer pageSize) {
        PageDomain page = PageDomain.of(pageNum, pageSize);
        long total = sysScreenSizeMapper.countSysScreenSizeList(query);
        List<SysScreenSize> rows = sysScreenSizeMapper.selectSysScreenSizeList(
                query, page.getOffset(), page.getPageSize());
        return new TableDataInfo(rows, total);
    }

    public List<SysScreenSize> selectScreenSizeListAll(SysScreenSize query) {
        List<SysScreenSize> list = sysScreenSizeMapper.selectSysScreenSizeListAll(query);
        return list != null ? list : List.of();
    }

    public SysScreenSize selectSysScreenSizeByScreenSizeId(Long screenSizeId) {
        return sysScreenSizeMapper.selectSysScreenSizeByScreenSizeId(screenSizeId);
    }

    @Transactional
    public int insertSysScreenSize(SysScreenSize sysScreenSize) {
        return sysScreenSizeMapper.insertSysScreenSize(sysScreenSize);
    }

    @Transactional
    public int updateSysScreenSize(SysScreenSize sysScreenSize) {
        return sysScreenSizeMapper.updateSysScreenSize(sysScreenSize);
    }

    @Transactional
    public int deleteSysScreenSizeByScreenSizeIds(Long[] screenSizeIds) {
        return sysScreenSizeMapper.deleteSysScreenSizeByScreenSizeIds(screenSizeIds);
    }
}
