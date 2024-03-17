package com.cat2bug.api.service;

import com.cat2bug.common.core.domain.entity.SysDefect;
import com.cat2bug.common.core.domain.entity.SysReport;

import java.util.List;

/**
 * 报告Service接口
 * 
 * @author yuzhantao
 * @date 2024-03-13
 */
public interface IApiReportService
{
    /**
     * 推送报告
     * 
     * @param apiReport 报告
     * @return 结果
     */
    public int pushDefectReport(SysReport<List<SysDefect>> apiReport);
}
