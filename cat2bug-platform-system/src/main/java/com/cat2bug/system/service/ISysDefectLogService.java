package com.cat2bug.system.service;

import java.util.List;
import com.cat2bug.system.domain.SysDefectLog;

/**
 * 缺陷日志Service接口
 * 
 * @author yuzhantao
 * @date 2023-11-23
 */
public interface ISysDefectLogService 
{
    /**
     * 查询缺陷日志
     * 
     * @param defectLogId 缺陷日志主键
     * @return 缺陷日志
     */
    public SysDefectLog selectSysDefectLogByDefectLogId(Long defectLogId);

    /**
     * 查询缺陷日志列表
     * 
     * @param sysDefectLog 缺陷日志
     * @return 缺陷日志集合
     */
    public List<SysDefectLog> selectSysDefectLogList(SysDefectLog sysDefectLog);

    /**
     * 新增缺陷日志
     * 
     * @param sysDefectLog 缺陷日志
     * @return 结果
     */
    public int insertSysDefectLog(SysDefectLog sysDefectLog);

    /**
     * 修改缺陷日志
     * 
     * @param sysDefectLog 缺陷日志
     * @return 结果
     */
    public int updateSysDefectLog(SysDefectLog sysDefectLog);

    /**
     * 批量删除缺陷日志
     * 
     * @param defectLogIds 需要删除的缺陷日志主键集合
     * @return 结果
     */
    public int deleteSysDefectLogByDefectLogIds(Long[] defectLogIds);

    /**
     * 删除缺陷日志信息
     * 
     * @param defectLogId 缺陷日志主键
     * @return 结果
     */
    public int deleteSysDefectLogByDefectLogId(Long defectLogId);
}
