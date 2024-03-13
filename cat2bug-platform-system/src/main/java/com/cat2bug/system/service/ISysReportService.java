package com.cat2bug.system.service;

import java.util.List;
import com.cat2bug.system.domain.SysReport;

/**
 * 报告Service接口
 * 
 * @author yuzhantao
 * @date 2024-03-13
 */
public interface ISysReportService 
{
    /**
     * 查询报告
     * 
     * @param reportId 报告主键
     * @return 报告
     */
    public SysReport selectSysReportByReportId(Long reportId);

    /**
     * 查询报告列表
     * 
     * @param sysReport 报告
     * @return 报告集合
     */
    public List<SysReport> selectSysReportList(SysReport sysReport);

    /**
     * 新增报告
     * 
     * @param sysReport 报告
     * @return 结果
     */
    public int insertSysReport(SysReport sysReport);

    /**
     * 修改报告
     * 
     * @param sysReport 报告
     * @return 结果
     */
    public int updateSysReport(SysReport sysReport);

    /**
     * 批量删除报告
     * 
     * @param reportIds 需要删除的报告主键集合
     * @return 结果
     */
    public int deleteSysReportByReportIds(Long[] reportIds);

    /**
     * 删除报告信息
     * 
     * @param reportId 报告主键
     * @return 结果
     */
    public int deleteSysReportByReportId(Long reportId);
}
