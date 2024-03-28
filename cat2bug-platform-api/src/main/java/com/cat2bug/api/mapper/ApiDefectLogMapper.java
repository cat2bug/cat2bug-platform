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
     * 新增缺陷日志
     * 
     * @param sysDefectLog 缺陷日志
     * @return 结果
     */
    public int insertApiDefectLog(ApiDefectLog sysDefectLog);

    public int batchInsertApiDefectLog(List<ApiDefectLog> defectLogList);
}
