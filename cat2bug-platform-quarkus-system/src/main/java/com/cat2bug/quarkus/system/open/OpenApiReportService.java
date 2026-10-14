package com.cat2bug.quarkus.system.open;

import com.cat2bug.quarkus.common.domain.LoginUser;
import com.cat2bug.quarkus.common.domain.SysReport;
import com.cat2bug.quarkus.common.domain.open.ApiDefectLog;
import com.cat2bug.quarkus.common.domain.open.ApiDefectRequest;
import com.cat2bug.quarkus.common.domain.open.ApiMember;
import com.cat2bug.quarkus.common.domain.open.OpenApiDefectReportRequest;
import com.cat2bug.quarkus.common.domain.open.type.ApiDefectLogStateEnum;
import com.cat2bug.quarkus.common.domain.open.type.ApiDefectStateEnum;
import com.cat2bug.quarkus.common.domain.open.type.ApiDefectTypeEnum;
import com.cat2bug.quarkus.common.exception.ApiException;
import com.cat2bug.quarkus.common.util.StringUtils;
import com.cat2bug.quarkus.framework.security.AuthContext;
import com.cat2bug.quarkus.system.mapper.OpenApiDefectLogMapper;
import com.cat2bug.quarkus.system.mapper.OpenApiDefectMapper;
import com.cat2bug.quarkus.system.mapper.OpenApiMemberMapper;
import com.cat2bug.quarkus.system.mapper.OpenApiReportMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@ApplicationScoped
public class OpenApiReportService {

    private static final long DEFECT_DESCRIBE_MAX_LEN = 65536;
    private static final long DEFECT_NAME_MAX_LEN = 128;

    @Inject
    AuthContext authContext;

    @Inject
    OpenApiContextService openApiContextService;

    @Inject
    OpenApiReportMapper openApiReportMapper;

    @Inject
    OpenApiDefectMapper openApiDefectMapper;

    @Inject
    OpenApiMemberMapper openApiMemberMapper;

    @Inject
    OpenApiDefectLogMapper openApiDefectLogMapper;

    @Transactional
    public int pushDefectReport(OpenApiDefectReportRequest request) {
        Long projectId = openApiContextService.requireProjectId();
        LoginUser loginUser = requireLoginUser();

        SysReport report = toSysReport(request);
        report.setCreateById(loginUser.getUserId());
        report.setProjectId(projectId);

        int ret;
        if (StringUtils.isNotBlank(report.getReportKey())) {
            SysReport oldReport = openApiReportMapper.findSysReportByReportKey(report.getReportKey());
            if (oldReport != null) {
                ret = openApiReportMapper.updateSysReport(report);
            } else {
                ret = openApiReportMapper.insertSysReport(report);
            }
        } else {
            ret = openApiReportMapper.insertSysReport(report);
        }

        List<ApiDefectRequest> list = request.getReportData();
        if (ret > 0 && list != null) {
            List<Long> handlerIds = resolveHandlerIds(projectId, request.getHandler());
            List<ApiDefectRequest> insertList = list.stream()
                    .filter(l -> StringUtils.isBlank(l.getDefectGroupKey()) && StringUtils.isBlank(l.getDefectKey()))
                    .collect(Collectors.toList());
            if (!insertList.isEmpty()) {
                insertDefects(projectId, loginUser, handlerIds, insertList);
            }

            List<ApiDefectRequest> updateList = list.stream()
                    .filter(l -> StringUtils.isNotBlank(l.getDefectGroupKey()) || StringUtils.isNotBlank(l.getDefectKey()))
                    .collect(Collectors.toList());
            if (!updateList.isEmpty()) {
                updateDefects(projectId, loginUser, handlerIds, updateList);
            }
        }
        return ret;
    }

    private SysReport toSysReport(OpenApiDefectReportRequest request) {
        SysReport report = new SysReport();
        report.setReportId(request.getReportId());
        report.setReportTitle(request.getReportTitle());
        report.setReportTime(request.getReportTime());
        report.setReportDescription(request.getReportDescription());
        report.setReportDataCoder(request.getReportDataCoder());
        report.setReportData(request.getReportData());
        report.setReportSource(request.getReportSource());
        report.setReportKey(request.getReportKey());
        return report;
    }

    private List<Long> resolveHandlerIds(Long projectId, String handler) {
        List<Long> handlerIds = new ArrayList<>();
        if (StringUtils.isNotBlank(handler)) {
            List<ApiMember> members = openApiMemberMapper.selectMemberByNames(projectId, Arrays.asList(handler));
            Optional<ApiMember> member = members.stream().findFirst();
            member.ifPresent(apiMember -> handlerIds.add(apiMember.getMemberId()));
        }
        return handlerIds;
    }

    private void insertDefects(Long projectId, LoginUser loginUser, List<Long> handlerIds, List<ApiDefectRequest> list) {
        for (List<ApiDefectRequest> batch : partition(list, 50)) {
            AtomicLong count = new AtomicLong(openApiDefectMapper.getProjectDefectMaxNum(projectId));
            for (ApiDefectRequest defect : batch) {
                setInsertSysDefect(defect, count.incrementAndGet(), handlerIds, loginUser);
                openApiDefectMapper.insertApiDefect(projectId, defect);
            }
            batchInsertLogs(batch, loginUser);
        }
    }

    private void setInsertSysDefect(ApiDefectRequest sysDefect, long projectNumber, List<Long> handlerIds,
                                    LoginUser loginUser) {
        if (sysDefect.getDefectName() != null) {
            sysDefect.setDefectName(sysDefect.getDefectName().substring(0,
                    (int) Math.min(sysDefect.getDefectName().length(), DEFECT_NAME_MAX_LEN)));
        }
        if (sysDefect.getDefectDescribe() != null) {
            sysDefect.setDefectDescribe(sysDefect.getDefectDescribe().substring(0,
                    (int) Math.min(sysDefect.getDefectDescribe().length(), DEFECT_DESCRIBE_MAX_LEN)));
        }

        Date now = new Date();
        sysDefect.setCreateById(loginUser.getUserId());
        sysDefect.setCreateTime(now);
        sysDefect.setUpdateTime(now);
        sysDefect.setUpdateById(loginUser.getUserId());
        sysDefect.setHandleBy(handlerIds);
        if (sysDefect.getDefectType() == null) {
            sysDefect.setDefectType(ApiDefectTypeEnum.BUG);
        }
        if (sysDefect.getDefectState() == null) {
            sysDefect.setDefectState(ApiDefectStateEnum.PROCESSING);
        }
        if (StringUtils.isBlank(sysDefect.getDefectLevel())) {
            sysDefect.setDefectLevel("middle");
        } else {
            sysDefect.setDefectLevel(sysDefect.getDefectLevel().toLowerCase());
        }
    }

    private void updateDefects(Long projectId, LoginUser loginUser, List<Long> handlerIds, List<ApiDefectRequest> list) {
        for (ApiDefectRequest defect : list) {
            List<Long> ids = openApiDefectMapper.selectDefectIdsByKey(
                    projectId, defect.getDefectGroupKey(), defect.getDefectKey());
            if (ids.isEmpty()) {
                if (StringUtils.isBlank(defect.getDefectName()) || StringUtils.isBlank(defect.getDefectDescribe())) {
                    continue;
                }
                long count = openApiDefectMapper.getProjectDefectMaxNum(projectId);
                setInsertSysDefect(defect, count + 1, handlerIds, loginUser);
                openApiDefectMapper.insertApiDefect(projectId, defect);
                insertLog(defect, loginUser, ApiDefectLogStateEnum.CREATE);
            } else {
                for (Long id : ids) {
                    defect.setDefectId(id);
                    defect.setUpdateTime(new Date());
                    defect.setHandleBy(handlerIds);
                    openApiDefectMapper.updateApiDefect(defect);
                    insertLog(defect, loginUser, ApiDefectLogStateEnum.UPDATE);
                }
            }
        }
    }

    private ApiDefectLog toLog(ApiDefectRequest sysDefect, ApiDefectLogStateEnum logStateEnum, LoginUser loginUser) {
        ApiDefectLog sysDefectLog = new ApiDefectLog();
        sysDefectLog.setDefectId(sysDefect.getDefectId());
        sysDefectLog.setDefectLogDescribe("");
        sysDefectLog.setDefectLogState(logStateEnum);
        sysDefectLog.setCreateBy(loginUser.getUserId().toString());
        return sysDefectLog;
    }

    private void batchInsertLogs(List<ApiDefectRequest> list, LoginUser loginUser) {
        Date now = new Date();
        List<ApiDefectLog> logList = list.stream().map(l -> {
            ApiDefectLog log = toLog(l, ApiDefectLogStateEnum.CREATE, loginUser);
            log.setCreateById(loginUser.getUserId());
            log.setCreateTime(now);
            return log;
        }).collect(Collectors.toList());
        openApiDefectLogMapper.batchInsertApiDefectLog(logList);
    }

    private ApiDefectLog insertLog(ApiDefectRequest sysDefect, LoginUser loginUser, ApiDefectLogStateEnum logStateEnum) {
        ApiDefectLog sysDefectLog = toLog(sysDefect, logStateEnum, loginUser);
        if (sysDefectLog.getDefectId() == null) {
            throw new ApiException("缺陷ID不能为空");
        }
        sysDefectLog.setCreateById(loginUser.getUserId());
        sysDefectLog.setCreateTime(new Date());
        return openApiDefectLogMapper.insertApiDefectLog(sysDefectLog) > 0 ? sysDefectLog : null;
    }

    private LoginUser requireLoginUser() {
        LoginUser loginUser = authContext.getLoginUser();
        if (loginUser == null || loginUser.getUserId() == null) {
            throw new ApiException(401, "未授权");
        }
        return loginUser;
    }

    private static <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> parts = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            parts.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return parts;
    }
}
