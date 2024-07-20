package com.cat2bug.system.service.impl;

import java.util.*;
import java.util.stream.Collectors;

import com.cat2bug.common.core.domain.entity.SysDefect;
import com.cat2bug.common.utils.DateUtils;
import com.cat2bug.common.utils.MessageUtils;
import com.cat2bug.common.utils.SecurityUtils;
import com.cat2bug.im.domain.IMUserConfig;
import com.cat2bug.im.mapper.IMUserConfigMapper;
import com.cat2bug.im.service.IMService;
import com.cat2bug.system.domain.SysUserDefect;
import com.cat2bug.system.domain.SysUserProject;
import com.cat2bug.system.service.IReportParseService;
import com.cat2bug.system.service.ISysCaseService;
import com.cat2bug.system.service.ISysUserProjectService;
import lombok.extern.log4j.Log4j;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
    private final static Logger log = LogManager.getLogger(SysReportServiceImpl.class);
    private final static String ENTER_CHAR = "\n";
    @Autowired
    private SysReportMapper sysReportMapper;
    @Autowired
    private IMUserConfigMapper imUserConfigMapper;
    @Autowired
    private List<IReportParseService> reportParseServices;
    @Autowired
    private IMService imService;
    @Autowired
    private ISysUserProjectService sysUserProjectService;
    @Autowired
    private ReportMessageOfNoticeTemplateImpl reportMessageOfNoticeTemplate;
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

        int count = sysReportMapper.insertSysReport(sysReport);
        if(count>0) {
            this.sendDefectNotice(sysReport);
        }
        return count;
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

    /**
     * 发送缺陷通知
     * @param report
     */
    private void sendDefectNotice(SysReport report) {
        // 获取获取报告人的ID集合
        IMUserConfig imUserConfig = new IMUserConfig();
        imUserConfig.setProjectId(report.getProjectId());
        SysUserProject sysUserProject = new SysUserProject();
        sysUserProject.setProjectId(report.getProjectId());
        sysUserProject.setProjectLock(0);
        List<Long> memberIdList = sysUserProjectService.selectSysUserProjectList(sysUserProject).stream()
                .filter(u->{
                    try {
                        IMUserConfig userConfig = imUserConfigMapper.selectImUserConfigByProjectAndMember(u.getProjectId(), u.getUserId());
                        Map<String, Object> defectConfig = (Map<String, Object>) userConfig.getConfig().getModules().get("report");
                        Map<String, Object> eventConfig = (Map<String, Object>) defectConfig.get("event");
                        return (boolean) eventConfig.get("new");
                    }catch (Exception e) {
                        log.error(e);
                        return false;
                    }
                })
                .map(u->u.getUserId()).collect(Collectors.toList());
        try {
            // 给处理人和关注此缺陷的人发送通知
            String title = String.format("%s:%s",
                    MessageUtils.message("report"),
                    report.getReportTitle());
            this.imService.sendMessage(
                    report.getProjectId(),  // 项目ID
                    SysReport.KEY,  // 通知组名称
                    SecurityUtils.getUserId(),  // 发送人ID
                    memberIdList, // 接收人集合
                    title,      // 通知标题
                    report,     // 通知内容
                    String.format("%s/#/project/report?reportId=%d",report.getSrcHost(), report.getReportId()),
                    this.reportMessageOfNoticeTemplate  // 通知内容格式模版
            );
        }catch (Exception e) {
            log.error(e);
        }

    }
}
