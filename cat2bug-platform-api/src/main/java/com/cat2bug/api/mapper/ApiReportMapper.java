package com.cat2bug.api.mapper;

import com.cat2bug.common.core.domain.entity.SysReport;
import org.apache.ibatis.annotations.Mapper;

/**
 * 报告Mapper接口
 * 
 * @author yuzhantao
 * @date 2024-03-13
 */
@Mapper
public interface ApiReportMapper
{
    /**
     * 新增报告
     *
     * @param apiReport 报告
     * @return 结果
     */
    public int insertSysReport(SysReport apiReport);
}
