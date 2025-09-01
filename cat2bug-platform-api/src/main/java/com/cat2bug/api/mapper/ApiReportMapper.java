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
     * 查找报告
     * @param reportKey 报告key
     * @return  报告
     */
    public SysReport findSysReportByReportKey(String reportKey);

    /**
     * 新增报告
     * @param apiReport 报告
     * @return 新增数量
     */
    public int insertSysReport(SysReport apiReport);

    /**
     * 更新报告
     * @param apiReport 报告
     * @return  更新数量
     */
    public int updateSysReport(SysReport apiReport);
}
