package com.cat2bug.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.cat2bug.system.mapper.SysReportMapper;
import com.cat2bug.system.domain.SysReport;
import com.cat2bug.system.service.ISysReportService;

/**
 * 报告Service业务层处理
 * 
 * @author yuzhantao
 * @date 2024-03-13
 */
@Service
public class SysReportServiceImpl implements ISysReportService 
{
    @Autowired
    private SysReportMapper sysReportMapper;

    /**
     * 查询报告
     * 
     * @param reportId 报告主键
     * @return 报告
     */
    @Override
    public SysReport selectSysReportByReportId(Long reportId)
    {
        return sysReportMapper.selectSysReportByReportId(reportId);
    }

    /**
     * 查询报告列表
     * 
     * @param sysReport 报告
     * @return 报告
     */
    @Override
    public List<SysReport> selectSysReportList(SysReport sysReport)
    {
        return sysReportMapper.selectSysReportList(sysReport);
    }

    /**
     * 新增报告
     * 
     * @param sysReport 报告
     * @return 结果
     */
    @Override
    public int insertSysReport(SysReport sysReport)
    {
        return sysReportMapper.insertSysReport(sysReport);
    }

    /**
     * 修改报告
     * 
     * @param sysReport 报告
     * @return 结果
     */
    @Override
    public int updateSysReport(SysReport sysReport)
    {
        return sysReportMapper.updateSysReport(sysReport);
    }

    /**
     * 批量删除报告
     * 
     * @param reportIds 需要删除的报告主键
     * @return 结果
     */
    @Override
    public int deleteSysReportByReportIds(Long[] reportIds)
    {
        return sysReportMapper.deleteSysReportByReportIds(reportIds);
    }

    /**
     * 删除报告信息
     * 
     * @param reportId 报告主键
     * @return 结果
     */
    @Override
    public int deleteSysReportByReportId(Long reportId)
    {
        return sysReportMapper.deleteSysReportByReportId(reportId);
    }
}
