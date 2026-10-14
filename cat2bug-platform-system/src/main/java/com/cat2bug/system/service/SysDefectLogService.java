package com.cat2bug.system.service;

import com.cat2bug.common.api.TableDataInfo;
import com.cat2bug.common.core.page.PageDomain;
import com.cat2bug.common.domain.SysComment;
import com.cat2bug.common.domain.SysDefectLog;
import com.cat2bug.common.domain.SysUser;
import com.cat2bug.system.mapper.SysCommentMapper;
import com.cat2bug.system.mapper.SysDefectLogMapper;
import com.cat2bug.system.mapper.SysUserMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class SysDefectLogService {

    public static final String COMMENT_TYPE = "defect_log";

    @Inject
    SysDefectLogMapper sysDefectLogMapper;

    @Inject
    SysCommentMapper sysCommentMapper;

    @Inject
    SysUserMapper sysUserMapper;

    public TableDataInfo selectLogPage(SysDefectLog query, Integer pageNum, Integer pageSize) {
        PageDomain page = PageDomain.of(pageNum, pageSize);
        long total = sysDefectLogMapper.countSysDefectLogList(query);
        List<SysDefectLog> rows = sysDefectLogMapper.selectSysDefectLogList(query, page.getOffset(), page.getPageSize());
        fillLogDetails(rows);
        return new TableDataInfo(rows, total);
    }

    public SysDefectLog selectSysDefectLogByDefectLogId(Long defectLogId) {
        SysDefectLog log = sysDefectLogMapper.selectSysDefectLogByDefectLogId(defectLogId);
        if (log != null) {
            fillLogDetails(Collections.singletonList(log));
        }
        return log;
    }

    public List<SysDefectLog> selectSysDefectLogList(SysDefectLog query) {
        List<SysDefectLog> list = sysDefectLogMapper.selectSysDefectLogList(query, 0, Integer.MAX_VALUE);
        fillLogDetails(list);
        return list;
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

    /** 批量填充接收人与评论，避免逐条查询。 */
    void fillLogDetails(List<SysDefectLog> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        List<Long> logIds = list.stream()
                .map(SysDefectLog::getDefectLogId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        Map<Long, List<SysComment>> commentsByLogId = logIds.isEmpty()
                ? Collections.emptyMap()
                : sysCommentMapper.selectSysCommentListByModuleAndCorrelationIds(COMMENT_TYPE, logIds).stream()
                        .collect(Collectors.groupingBy(SysComment::getCorrelationId));
        Map<Long, SysUser> userMap = loadReceiveUsers(list);
        for (SysDefectLog log : list) {
            log.setCommentList(commentsByLogId.getOrDefault(log.getDefectLogId(), Collections.emptyList()));
            log.setReceiveByList(buildReceiveByList(log.getReceiveBy(), userMap));
        }
    }

    private Map<Long, SysUser> loadReceiveUsers(List<SysDefectLog> list) {
        Set<Long> userIds = new LinkedHashSet<>();
        for (SysDefectLog log : list) {
            if (log.getReceiveBy() != null) {
                log.getReceiveBy().stream().filter(Objects::nonNull).forEach(userIds::add);
            }
        }
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, SysUser> userMap = new HashMap<>();
        for (SysUser user : sysUserMapper.selectUsersByIds(new ArrayList<>(userIds))) {
            userMap.put(user.getUserId(), user);
        }
        return userMap;
    }

    private static List<SysUser> buildReceiveByList(List<Long> receiveBy, Map<Long, SysUser> userMap) {
        if (receiveBy == null || receiveBy.isEmpty()) {
            return Collections.emptyList();
        }
        List<SysUser> result = new ArrayList<>(receiveBy.size());
        for (Long userId : receiveBy) {
            SysUser user = userMap.get(userId);
            if (user != null) {
                result.add(user);
            }
        }
        return result;
    }
}
