package com.cat2bug.system.service.impl;

import java.util.List;
import com.cat2bug.common.utils.DateUtils;
import com.cat2bug.system.domain.SysComment;
import com.cat2bug.system.mapper.SysCommentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.cat2bug.system.mapper.SysDefectLogMapper;
import com.cat2bug.system.domain.SysDefectLog;
import com.cat2bug.system.service.ISysDefectLogService;

/**
 * 缺陷日志Service业务层处理
 * 
 * @author yuzhantao
 * @date 2023-11-23
 */
@Service
public class SysDefectLogServiceImpl implements ISysDefectLogService 
{
    private final static String COMMENT_TYPE = "defect_log";

    @Autowired
    private SysDefectLogMapper sysDefectLogMapper;

    @Autowired
    private SysCommentMapper sysCommentMapper;

    /**
     * 查询缺陷日志
     * 
     * @param defectLogId 缺陷日志主键
     * @return 缺陷日志
     */
    @Override
    public SysDefectLog selectSysDefectLogByDefectLogId(Long defectLogId)
    {
        return sysDefectLogMapper.selectSysDefectLogByDefectLogId(defectLogId);
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
        list.forEach(l->{
            SysComment sysComment = new SysComment();
            sysComment.setModuleType(COMMENT_TYPE);
            sysComment.setCorrelationId(l.getDefectLogId());
            List<SysComment> commentList = sysCommentMapper.selectSysCommentList(sysComment);
            l.setCommentList(commentList);
        });
        return list;
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
