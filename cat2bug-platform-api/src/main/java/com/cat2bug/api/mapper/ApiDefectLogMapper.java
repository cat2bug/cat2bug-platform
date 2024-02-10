package com.cat2bug.api.mapper;

import com.cat2bug.api.domain.ApiDefectLog;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 缺陷日志Mapper接口
 * 
 * @author yuzhantao
 * @date 2023-11-23
 */
@Mapper
public interface ApiDefectLogMapper 
{
    /**
     * 查询缺陷日志
     * 
     * @param defectLogId 缺陷日志主键
     * @return 缺陷日志
     */
    public ApiDefectLog selectApiDefectLogByDefectLogId(Long defectLogId);

    /**
     * 查询缺陷日志列表
     * 
     * @param sysDefectLog 缺陷日志
     * @return 缺陷日志集合
     */
    public List<ApiDefectLog> selectApiDefectLogList(ApiDefectLog sysDefectLog);

    /**
     * 新增缺陷日志
     * 
     * @param sysDefectLog 缺陷日志
     * @return 结果
     */
    public int insertApiDefectLog(ApiDefectLog sysDefectLog);

    /**
     * 修改缺陷日志
     * 
     * @param sysDefectLog 缺陷日志
     * @return 结果
     */
    public int updateApiDefectLog(ApiDefectLog sysDefectLog);

    /**
     * 删除缺陷日志
     * 
     * @param defectLogId 缺陷日志主键
     * @return 结果
     */
    public int deleteApiDefectLogByDefectLogId(Long defectLogId);

    /**
     * 批量删除缺陷日志
     * 
     * @param defectLogIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteApiDefectLogByDefectLogIds(Long[] defectLogIds);
}
