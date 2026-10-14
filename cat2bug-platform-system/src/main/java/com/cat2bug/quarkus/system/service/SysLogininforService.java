package com.cat2bug.quarkus.system.service;

import com.cat2bug.quarkus.common.api.TableDataInfo;
import com.cat2bug.quarkus.common.core.page.PageDomain;
import com.cat2bug.quarkus.common.domain.SysLogininfor;
import com.cat2bug.quarkus.system.mapper.SysLogininforMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
public class SysLogininforService {

    @Inject
    SysLogininforMapper sysLogininforMapper;

    public void insertLogininfor(SysLogininfor logininfor) {
        sysLogininforMapper.insertLogininfor(logininfor);
    }

    public TableDataInfo selectLogininforPage(SysLogininfor query, Integer pageNum, Integer pageSize) {
        PageDomain page = PageDomain.of(pageNum, pageSize);
        long total = sysLogininforMapper.countLogininforList(query);
        List<SysLogininfor> rows = sysLogininforMapper.selectLogininforList(query, page.getOffset(), page.getPageSize());
        return new TableDataInfo(rows, total);
    }

    public List<SysLogininfor> selectLogininforListAll(SysLogininfor query) {
        List<SysLogininfor> list = sysLogininforMapper.selectLogininforListAll(query);
        return list != null ? list : List.of();
    }

    @Transactional
    public int deleteLogininforByIds(Long[] infoIds) {
        return sysLogininforMapper.deleteLogininforByIds(infoIds);
    }

    @Transactional
    public int cleanLogininfor() {
        return sysLogininforMapper.cleanLogininfor();
    }
}
