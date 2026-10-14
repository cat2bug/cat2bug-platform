package com.cat2bug.quarkus.system.service;

import com.cat2bug.quarkus.common.api.TableDataInfo;
import com.cat2bug.quarkus.common.core.page.PageDomain;
import com.cat2bug.quarkus.common.domain.SysNotice;
import com.cat2bug.quarkus.common.util.IdUtils;
import com.cat2bug.quarkus.common.util.StringUtils;
import com.cat2bug.quarkus.system.mapper.SysNoticeMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Map;

@ApplicationScoped
public class SysNoticeService {

    @Inject
    SysNoticeMapper sysNoticeMapper;

    public List<Map<String, Object>> noticeGroupStatistics(Long receiverId) {
        return sysNoticeMapper.noticeGroupStatistics(receiverId);
    }

    public TableDataInfo selectNoticePage(SysNotice query, Integer pageNum, Integer pageSize) {
        PageDomain page = PageDomain.of(pageNum, pageSize);
        long total = sysNoticeMapper.countNoticeList(query);
        List<SysNotice> rows = sysNoticeMapper.selectNoticeList(query, page.getOffset(), page.getPageSize());
        return new TableDataInfo(rows, total);
    }

    public SysNotice selectNoticeById(String noticeId) {
        return sysNoticeMapper.selectNoticeById(noticeId);
    }

    public int insertNotice(SysNotice notice) {
        if (StringUtils.isBlank(notice.getNoticeId())) {
            notice.setNoticeId(IdUtils.fastSimpleUUID());
        }
        return sysNoticeMapper.insertNotice(notice);
    }

    public int insertReceiverNotice(SysNotice notice) {
        return sysNoticeMapper.insertReceiverNotice(notice);
    }

    public int updateNotice(SysNotice notice) {
        return sysNoticeMapper.updateNotice(notice);
    }

    public int deleteNoticeByIds(String[] noticeIds) {
        return sysNoticeMapper.deleteNoticeByIds(noticeIds);
    }
}
