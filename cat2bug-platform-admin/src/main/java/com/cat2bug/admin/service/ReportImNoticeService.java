package com.cat2bug.admin.service;

import com.cat2bug.admin.service.im.ImEvent;
import com.cat2bug.admin.service.im.ImLinkBuilder;
import com.cat2bug.admin.service.im.ImMessageTemplates;
import com.cat2bug.common.domain.LoginUser;
import com.cat2bug.common.domain.SysReport;
import com.cat2bug.common.domain.SysUserProject;
import com.cat2bug.common.domain.im.IMUserConfig;
import com.cat2bug.framework.security.AuthContext;
import com.cat2bug.system.mapper.SysUserProjectMapper;
import com.cat2bug.system.service.ImUserConfigService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 报告新建后通知项目成员（对齐 Spring {@code SysReportServiceImpl#sendDefectNotice}）：
 * 只通知未锁定、且个人配置 {@code modules.report.event.new} 为真的成员。
 */
@ApplicationScoped
public class ReportImNoticeService {

    private static final Logger LOG = Logger.getLogger(ReportImNoticeService.class);
    static final String REPORT_GROUP = SysReport.KEY;

    @Inject
    SysUserProjectMapper sysUserProjectMapper;

    @Inject
    ImUserConfigService imUserConfigService;

    @Inject
    ImNoticeDispatchService imNoticeDispatchService;

    @Inject
    ImLinkBuilder imLinkBuilder;

    @Inject
    AuthContext authContext;

    public void sendReportNotice(SysReport report) {
        try {
            if (report == null || report.getProjectId() == null || report.getReportId() == null) {
                return;
            }
            List<Long> receiverIds = selectReceivers(report.getProjectId());
            if (receiverIds.isEmpty()) {
                return;
            }
            String link = imLinkBuilder.report(report.getSrcHost(), report.getProjectId(), report.getReportId());
            String title = "报告:" + (report.getReportTitle() != null ? report.getReportTitle() : "");
            imNoticeDispatchService.dispatch(new ImEvent(report.getProjectId(), REPORT_GROUP, resolveSenderId(),
                    receiverIds, title, link, ImMessageTemplates.report(report, link), REPORT_GROUP, null));
        } catch (Exception ex) {
            LOG.warnf(ex, "发送报告通知失败 reportId=%s", report != null ? report.getReportId() : null);
        }
    }

    List<Long> selectReceivers(Long projectId) {
        SysUserProject query = new SysUserProject();
        query.setProjectId(projectId);
        List<SysUserProject> members = sysUserProjectMapper.selectSysUserProjectList(query);
        if (members == null || members.isEmpty()) {
            return List.of();
        }
        Set<Long> receivers = new LinkedHashSet<>();
        for (SysUserProject member : members) {
            if (member.getUserId() == null || (member.getProjectLock() != null && member.getProjectLock() != 0)) {
                continue;
            }
            if (isNewReportEnabled(projectId, member.getUserId())) {
                receivers.add(member.getUserId());
            }
        }
        return new ArrayList<>(receivers);
    }

    private boolean isNewReportEnabled(Long projectId, Long memberId) {
        try {
            IMUserConfig userConfig = imUserConfigService.selectImUserConfigByProjectAndMember(projectId, memberId, null);
            Map<String, Object> modules = userConfig.getConfig().getModules();
            Object report = modules.get(REPORT_GROUP);
            if (!(report instanceof Map<?, ?> reportMap)) {
                return false;
            }
            Object event = reportMap.get("event");
            return event instanceof Map<?, ?> eventMap && Boolean.TRUE.equals(eventMap.get("new"));
        } catch (Exception ex) {
            LOG.debugf(ex, "读取 IM 报告通知配置失败 projectId=%d memberId=%d", projectId, memberId);
            return false;
        }
    }

    private Long resolveSenderId() {
        LoginUser loginUser = authContext.getLoginUser();
        return loginUser != null ? loginUser.getUserId() : null;
    }
}
