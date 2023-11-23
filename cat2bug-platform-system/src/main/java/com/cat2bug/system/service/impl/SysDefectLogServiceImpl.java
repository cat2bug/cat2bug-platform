package com.cat2bug.system.service.impl;

import java.util.List;
import com.cat2bug.common.utils.DateUtils;
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
    @Autowired
    private SysDefectLogMapper sysDefectLogMapper;

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
        return sysDefectLogMapper.selectSysDefectLogList(sysDefectLog);
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
