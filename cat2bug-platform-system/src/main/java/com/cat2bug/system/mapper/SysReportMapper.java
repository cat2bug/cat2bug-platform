package com.cat2bug.system.mapper;

import java.util.List;
import com.cat2bug.system.domain.SysReport;

/**
 * 报告Mapper接口
 * 
 * @author yuzhantao
 * @date 2024-03-13
 */
public interface SysReportMapper 
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
     * 删除报告
     * 
     * @param reportId 报告主键
     * @return 结果
     */
    public int deleteSysReportByReportId(Long reportId);

    /**
     * 批量删除报告
     * 
     * @param reportIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteSysReportByReportIds(Long[] reportIds);
}
