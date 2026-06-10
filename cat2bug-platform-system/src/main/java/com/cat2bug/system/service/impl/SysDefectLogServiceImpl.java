package com.cat2bug.system.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.common.utils.DateUtils;
import com.cat2bug.system.domain.SysComment;
import com.cat2bug.system.domain.SysDefectLog;
import com.cat2bug.system.mapper.SysCommentMapper;
import com.cat2bug.system.mapper.SysDefectLogMapper;
import com.cat2bug.system.mapper.SysUserMapper;
import com.cat2bug.system.service.ISysDefectLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 缺陷日志Service业务层处理
 * 
 * @author yuzhantao
 * @date 2023-11-23
 */
@Service
public class SysDefectLogServiceImpl implements ISysDefectLogService 
{
    public final static String COMMENT_TYPE = "defect_log";

    @Autowired
    private SysDefectLogMapper sysDefectLogMapper;

    @Autowired
    private SysCommentMapper sysCommentMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    /**
     * 查询缺陷日志
     * 
     * @param defectLogId 缺陷日志主键
     * @return 缺陷日志
     */
    @Override
    public SysDefectLog selectSysDefectLogByDefectLogId(Long defectLogId)
    {
        SysDefectLog sysDefectLog = sysDefectLogMapper.selectSysDefectLogByDefectLogId(defectLogId);
        if (sysDefectLog != null) {
            fillLogDetails(Collections.singletonList(sysDefectLog));
        }
        return sysDefectLog;
    }

    /**
     * 查询缺陷日志列表
     * 
     * @param sysDefectLog 缺陷日志
     * @return 缺陷日志
     */
    @Override
    public List<SysDefectLog> selectSysDefectLogList(SysDefectLog sysDefectLog)
    {
        List<SysDefectLog> list = sysDefectLogMapper.selectSysDefectLogList(sysDefectLog);
        fillLogDetails(list);
        return list;
    }

    /**
     * 批量填充接收人与评论，避免 N+1 查询及全表 REGEXP 子查询
     */
    private void fillLogDetails(List<SysDefectLog> list)
    {
        if (list == null || list.isEmpty()) {
            return;
        }

        List<Long> logIds = list.stream()
                .map(SysDefectLog::getDefectLogId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Map<Long, List<SysComment>> commentsByLogId = loadCommentsByLogIds(logIds);
        Map<Long, SysUser> userMap = loadReceiveUsers(list);

        for (SysDefectLog log : list) {
            log.setCommentList(commentsByLogId.getOrDefault(log.getDefectLogId(), Collections.emptyList()));
            log.setReceiveByList(buildReceiveByList(log.getReceiveBy(), userMap));
        }
    }

    private Map<Long, List<SysComment>> loadCommentsByLogIds(List<Long> logIds)
    {
        if (logIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<SysComment> comments = sysCommentMapper.selectSysCommentListByModuleAndCorrelationIds(COMMENT_TYPE, logIds);
        return comments.stream().collect(Collectors.groupingBy(SysComment::getCorrelationId));
    }

    private Map<Long, SysUser> loadReceiveUsers(List<SysDefectLog> list)
    {
        Set<Long> userIds = new HashSet<>();
        for (SysDefectLog log : list) {
            if (log.getReceiveBy() != null) {
                userIds.addAll(log.getReceiveBy());
            }
        }
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<SysUser> users = sysUserMapper.selectUsersByUserIds(new ArrayList<>(userIds));
        Map<Long, SysUser> userMap = new HashMap<>(users.size());
        for (SysUser user : users) {
            userMap.put(user.getUserId(), user);
        }
        return userMap;
    }

    private List<SysUser> buildReceiveByList(List<Long> receiveBy, Map<Long, SysUser> userMap)
    {
        if (receiveBy == null || receiveBy.isEmpty()) {
            return Collections.emptyList();
        }
        List<SysUser> receiveByList = new ArrayList<>(receiveBy.size());
        for (Long userId : receiveBy) {
            SysUser user = userMap.get(userId);
            if (user != null) {
                receiveByList.add(user);
            }
        }
        return receiveByList;
    }

    /**
     * 新增缺陷日志
     * 
     * @param sysDefectLog 缺陷日志
     * @return 结果
     */
    @Override
    public int insertSysDefectLog(SysDefectLog sysDefectLog)
    {
        sysDefectLog.setCreateTime(DateUtils.getNowDate());
        return sysDefectLogMapper.insertSysDefectLog(sysDefectLog);
    }

    /**
     * 修改缺陷日志
     * 
     * @param sysDefectLog 缺陷日志
     * @return 结果
     */
    @Override
    public int updateSysDefectLog(SysDefectLog sysDefectLog)
    {
        return sysDefectLogMapper.updateSysDefectLog(sysDefectLog);
    }

    /**
     * 批量删除缺陷日志
     * 
     * @param defectLogIds 需要删除的缺陷日志主键
     * @return 结果
     */
    @Override
    public int deleteSysDefectLogByDefectLogIds(Long[] defectLogIds)
    {
        return sysDefectLogMapper.deleteSysDefectLogByDefectLogIds(defectLogIds);
    }

    /**
     * 删除缺陷日志信息
     * 
     * @param defectLogId 缺陷日志主键
     * @return 结果
     */
    @Override
    public int deleteSysDefectLogByDefectLogId(Long defectLogId)
    {
        return sysDefectLogMapper.deleteSysDefectLogByDefectLogId(defectLogId);
    }
}
