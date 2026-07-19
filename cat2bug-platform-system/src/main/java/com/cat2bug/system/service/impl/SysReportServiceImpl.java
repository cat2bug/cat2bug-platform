package com.cat2bug.system.service.impl;

import java.util.*;
import java.util.stream.Collectors;

import com.cat2bug.common.utils.DateUtils;
import com.cat2bug.common.utils.MessageUtils;
import com.cat2bug.common.utils.SecurityUtils;
import com.cat2bug.im.domain.IMUserConfig;
import com.cat2bug.im.mapper.IMUserConfigMapper;
import com.cat2bug.im.service.IMService;
import com.cat2bug.system.domain.SysUserProject;
import com.cat2bug.system.service.IReportParseService;
import com.cat2bug.system.service.ISysUserProjectService;
import com.cat2bug.system.service.impl.report.parse.ReportParseUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.cat2bug.system.domain.SysReportHistory;
import com.cat2bug.system.mapper.SysReportHistoryMapper;
import com.cat2bug.system.mapper.SysReportMapper;
import com.cat2bug.common.core.domain.entity.SysReport;
import com.cat2bug.system.service.ISysReportService;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * 报告Service业务层处理
 *
 * @author yuzhantao
 * @date 2024-03-13
 */
@Service
@SuppressWarnings({"rawtypes", "unchecked"})
public class SysReportServiceImpl implements ISysReportService
{
    private final static Logger log = LogManager.getLogger(SysReportServiceImpl.class);
    @Autowired
    private SysReportMapper sysReportMapper;
    @Autowired
    private SysReportHistoryMapper sysReportHistoryMapper;
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

    @Override
    public SysReport selectSysReportByReportId(Long reportId)
    {
        SysReport report = sysReportMapper.selectSysReportByReportId(reportId);
        rematerializeIfNeeded(report);
        return report;
    }

    @Override
    public List<SysReport> selectSysReportList(SysReport sysReport)
    {
        return sysReportMapper.selectSysReportList(sysReport);
    }

    @Override
    public int insertSysReport(SysReport sysReport)
    {
        if(sysReport.getReportTime()==null){
            sysReport.setReportTime(DateUtils.getNowDate());
        }
        sysReport.setCreateById(SecurityUtils.getUserId());

        if(sysReport.getReportDescription()!=null) {
            sysReport.setReportDescription(materializeDescription(
                    sysReport.getProjectId(),
                    sysReport.getReportDescription(),
                    sysReport.getParams()));
        }

        int count = sysReportMapper.insertSysReport(sysReport);
        if(count>0) {
            this.sendDefectNotice(sysReport);
        }
        return count;
    }

    /**
     * Expand template DSL tokens in report description to static markdown/HTML.
     */
    public String materializeDescription(Long projectId, String description, Map<String, Object> params) {
        if (description == null || description.isEmpty()) {
            return description;
        }
        String content = description;
        Map<String, Object> safeParams = ReportParseUtils.safeParams(params);
        for (IReportParseService reportParseService : reportParseServices) {
            if (reportParseService.isHandle(content)) {
                content = reportParseService.parse(projectId, content, safeParams);
            }
        }
        return content;
    }

    /**
     * Lazily rematerialize legacy reports that still contain DSL tokens.
     * Persists without writing edit history.
     */
    public boolean rematerializeIfNeeded(SysReport report) {
        if (report == null || report.getReportId() == null) {
            return false;
        }
        String description = report.getReportDescription();
        if (!ReportParseUtils.containsReportDsl(description)) {
            return false;
        }
        String materialized = materializeDescription(report.getProjectId(), description, report.getParams());
        if (Objects.equals(materialized, description)) {
            return false;
        }
        report.setReportDescription(materialized);
        SysReport patch = new SysReport();
        patch.setReportId(report.getReportId());
        patch.setReportDescription(materialized);
        sysReportMapper.updateSysReport(patch);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateSysReport(SysReport sysReport)
    {
        String oldDescription = null;
        if (sysReport != null && sysReport.getReportId() != null && sysReport.getReportDescription() != null) {
            SysReport existing = sysReportMapper.selectSysReportByReportId(sysReport.getReportId());
            if (existing != null) {
                oldDescription = existing.getReportDescription();
            }
        }
        int count = sysReportMapper.updateSysReport(sysReport);
        if (count > 0 && sysReport != null && sysReport.getReportDescription() != null
                && !Objects.equals(oldDescription, sysReport.getReportDescription())) {
            SysReportHistory history = new SysReportHistory();
            history.setReportId(sysReport.getReportId());
            history.setReportDescription(sysReport.getReportDescription());
            history.setCreateById(SecurityUtils.getUserId());
            history.setCreateTime(DateUtils.getNowDate());
            sysReportHistoryMapper.insertSysReportHistory(history);
        }
        return count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteSysReportByReportIds(Long[] reportIds)
    {
        if (reportIds != null && reportIds.length > 0) {
            sysReportHistoryMapper.deleteSysReportHistoryByReportIds(reportIds);
        }
        return sysReportMapper.deleteSysReportByReportIds(reportIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteSysReportByReportId(Long reportId)
    {
        sysReportHistoryMapper.deleteSysReportHistoryByReportId(reportId);
        return sysReportMapper.deleteSysReportByReportId(reportId);
    }

    private void sendDefectNotice(SysReport report) {
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
            String title = String.format("%s:%s",
                    MessageUtils.message("report"),
                    report.getReportTitle());
            this.imService.sendMessage(
                    report.getProjectId(),
                    SysReport.KEY,
                    SecurityUtils.getUserId(),
                    memberIdList,
                    title,
                    report,
                    String.format("%s/#/project/report?reportId=%d",report.getSrcHost(), report.getReportId()),
                    this.reportMessageOfNoticeTemplate,
                    null
            );
        }catch (Exception e) {
            log.error(e);
        }
    }
}
