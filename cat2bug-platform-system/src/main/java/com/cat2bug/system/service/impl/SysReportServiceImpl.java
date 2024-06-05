package com.cat2bug.system.service.impl;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.cat2bug.common.utils.DateUtils;
import com.cat2bug.common.utils.SecurityUtils;
import com.cat2bug.system.service.IReportParseService;
import com.cat2bug.system.service.ISysCaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.cat2bug.system.mapper.SysReportMapper;
import com.cat2bug.common.core.domain.entity.SysReport;
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
    private final static String ENTER_CHAR = "\n";
    @Autowired
    private SysReportMapper sysReportMapper;
    @Autowired
    private List<IReportParseService> reportParseServices;
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
        if(sysReport.getReportTime()==null){
            sysReport.setReportTime(DateUtils.getNowDate());
        }
        sysReport.setCreateById(SecurityUtils.getUserId());

        if(sysReport.getReportDescription()!=null) {
            String[] lines = sysReport.getReportDescription().split(ENTER_CHAR);
            for(int i=0;i<lines.length;i++) {
                for(IReportParseService reportParseService : reportParseServices) {
                    if(reportParseService.isHandle(lines[i])) {
                        lines[i]=reportParseService.parse(sysReport.getProjectId(), lines[i], sysReport.getParams());
                    }
                }
            }
            sysReport.setReportDescription(Arrays.stream(lines).collect(Collectors.joining(ENTER_CHAR)));
        }

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
