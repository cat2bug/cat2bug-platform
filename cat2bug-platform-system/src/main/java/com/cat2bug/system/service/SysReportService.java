package com.cat2bug.system.service;

import com.cat2bug.common.api.TableDataInfo;
import com.cat2bug.common.core.page.PageDomain;
import com.cat2bug.common.domain.LoginUser;
import com.cat2bug.common.domain.SysReport;
import com.cat2bug.common.domain.SysReportHistory;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.framework.security.AuthContext;
import com.cat2bug.system.mapper.SysReportHistoryMapper;
import com.cat2bug.system.mapper.SysReportMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Objects;

@ApplicationScoped
public class SysReportService {

    @Inject
    SysReportMapper sysReportMapper;

    @Inject
    SysReportHistoryMapper sysReportHistoryMapper;

    @Inject
    AuthContext authContext;

    @Inject
    MemberFocusService memberFocusService;

    public TableDataInfo selectReportPage(SysReport query, Integer pageNum, Integer pageSize) {
        PageDomain page = PageDomain.of(pageNum, pageSize);
        long total = sysReportMapper.countSysReportList(query);
        List<SysReport> rows = sysReportMapper.selectSysReportList(query, page.getOffset(), page.getPageSize());
        return new TableDataInfo(rows, total);
    }

    public List<SysReport> selectReportListAll(SysReport query) {
        return sysReportMapper.selectSysReportList(query, 0, Integer.MAX_VALUE);
    }

    public SysReport selectSysReportByReportId(Long reportId) {
        SysReport report = sysReportMapper.selectSysReportByReportId(reportId);
        if (report == null) {
            throw new ApiException("报告不存在");
        }
        return report;
    }

    @Transactional
    public SysReport insertSysReport(SysReport sysReport) {
        LoginUser loginUser = requireLoginUser();
        if (sysReport.getReportTime() == null) {
            sysReport.setReportTime(new Date());
        }
        sysReport.setCreateById(loginUser.getUserId());
        if (sysReportMapper.insertSysReport(sysReport) <= 0) {
            throw new ApiException("创建报告失败");
        }
        return sysReport;
    }

    @Transactional
    public int updateSysReport(SysReport sysReport) {
        if (sysReport.getReportId() == null) {
            throw new ApiException("报告ID不能为空");
        }
        SysReport existing = selectSysReportByReportId(sysReport.getReportId());
        String oldDescription = existing.getReportDescription();
        int count = sysReportMapper.updateSysReport(sysReport);
        if (count > 0 && sysReport.getReportDescription() != null
                && !Objects.equals(oldDescription, sysReport.getReportDescription())) {
            LoginUser loginUser = authContext.getLoginUser();
            SysReportHistory history = new SysReportHistory();
            history.setReportId(sysReport.getReportId());
            history.setReportDescription(sysReport.getReportDescription());
            history.setCreateById(loginUser == null ? null : loginUser.getUserId());
            history.setCreateTime(new Date());
            sysReportHistoryMapper.insertSysReportHistory(history);
        }
        return count;
    }

    @Transactional
    public int deleteSysReportByReportIds(Long[] reportIds) {
        if (reportIds == null || reportIds.length == 0) {
            throw new ApiException("报告ID不能为空");
        }
        sysReportHistoryMapper.deleteSysReportHistoryByReportIds(reportIds);
        return sysReportMapper.deleteSysReportByReportIds(reportIds);
    }

    /** 关闭前端编辑窗口（清除成员编辑焦点）。 */
    public void closeEditWindow() {
        LoginUser loginUser = authContext.getLoginUser();
        if (loginUser != null) {
            memberFocusService.removeFocus(loginUser.getUserId());
        }
    }

    private LoginUser requireLoginUser() {
        LoginUser loginUser = authContext.getLoginUser();
        if (loginUser == null) {
            throw new ApiException(401, "未登录或会话已失效");
        }
        return loginUser;
    }
}
