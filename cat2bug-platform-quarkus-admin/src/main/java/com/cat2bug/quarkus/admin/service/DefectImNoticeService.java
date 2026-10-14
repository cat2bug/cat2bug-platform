package com.cat2bug.quarkus.admin.service;

import com.cat2bug.quarkus.common.constant.DefectConstants;
import com.cat2bug.quarkus.common.domain.LoginUser;
import com.cat2bug.quarkus.common.domain.SysDefect;
import com.cat2bug.quarkus.common.domain.SysUserDefect;
import com.cat2bug.quarkus.common.domain.im.IMUserConfig;
import com.cat2bug.quarkus.common.domain.vo.EnumVo;
import com.cat2bug.quarkus.common.domain.vo.SendNotice;
import com.cat2bug.quarkus.common.util.ImNoticeDefaults;
import com.cat2bug.quarkus.common.util.StringUtils;
import com.cat2bug.quarkus.framework.security.AuthContext;
import com.cat2bug.quarkus.system.mapper.SysUserDefectMapper;
import com.cat2bug.quarkus.system.service.ImUserConfigService;
import com.cat2bug.quarkus.system.service.SysDefectService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 缺陷变更后向处理人/关注人推送 IM 站内通知（对齐 Spring {@code SysDefectServiceImpl#sendDefectNotice}）。
 */
@ApplicationScoped
public class DefectImNoticeService {

    private static final Logger LOG = Logger.getLogger(DefectImNoticeService.class);
    static final String DEFECT_GROUP = "defect";

    @Inject
    SysDefectService sysDefectService;

    @Inject
    SysUserDefectMapper sysUserDefectMapper;

    @Inject
    ImUserConfigService imUserConfigService;

    @Inject
    ImNoticeDispatchService imNoticeDispatchService;

    @Inject
    AuthContext authContext;

    public void sendDefectNotice(String srcHost, Long defectId) {
        try {
            if (defectId == null) {
                return;
            }
            SysDefect defect = sysDefectService.selectSysDefectByDefectIdOptional(defectId);
            if (defect == null || defect.getProjectId() == null) {
                return;
            }
            Long projectId = defect.getProjectId();
            Set<Long> receiverIds = new LinkedHashSet<>();
            receiverIds.addAll(filterCollectUsers(projectId, defectId));
            receiverIds.addAll(filterHandleUsers(projectId, defect));
            if (receiverIds.isEmpty()) {
                return;
            }

            SendNotice notice = new SendNotice();
            notice.setProjectId(projectId);
            notice.setGroupName(DEFECT_GROUP);
            notice.setTitle(buildTitle(defect));
            notice.setContent(buildMarkdownContent(defect, srcHost));
            notice.setReceiveIds(new ArrayList<>(receiverIds));
            notice.setSrc(buildLink(defect, srcHost));
            imNoticeDispatchService.sendNotice(resolveSenderId(), notice);
        } catch (Exception ex) {
            LOG.warnf(ex, "发送缺陷通知失败 defectId=%d", defectId);
        }
    }

    private List<Long> filterCollectUsers(Long projectId, Long defectId) {
        SysUserDefect query = new SysUserDefect();
        query.setDefectId(defectId);
        query.setCollect(1);
        List<SysUserDefect> rows = sysUserDefectMapper.selectSysUserDefectList(query);
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        List<Long> ids = new ArrayList<>();
        for (SysUserDefect row : rows) {
            if (row.getUserId() == null) {
                continue;
            }
            if (isEventEnabled(projectId, row.getUserId(), "collect")) {
                ids.add(row.getUserId());
            }
        }
        return ids;
    }

    private List<Long> filterHandleUsers(Long projectId, SysDefect defect) {
        if (defect.getHandleBy() == null || defect.getHandleBy().isEmpty()) {
            return List.of();
        }
        List<Long> ids = new ArrayList<>();
        for (Long userId : defect.getHandleBy()) {
            if (userId == null) {
                continue;
            }
            if (isEventEnabled(projectId, userId, "receiver")) {
                ids.add(userId);
            }
        }
        return ids;
    }

    private boolean isEventEnabled(Long projectId, Long memberId, String eventKey) {
        try {
            IMUserConfig userConfig = imUserConfigService.selectImUserConfigByProjectAndMember(
                    projectId, memberId, ImNoticeDefaults.defaultDefectNoticeOption());
            return readEventFlag(userConfig, eventKey);
        } catch (Exception ex) {
            LOG.debugf(ex, "读取 IM 通知配置失败 projectId=%d memberId=%d", projectId, memberId);
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private static boolean readEventFlag(IMUserConfig userConfig, String eventKey) {
        if (userConfig == null || userConfig.getConfig() == null || userConfig.getConfig().getModules() == null) {
            return true;
        }
        Object defectModule = userConfig.getConfig().getModules().get("defect");
        if (!(defectModule instanceof Map<?, ?> defectMap)) {
            return true;
        }
        Object event = defectMap.get("event");
        if (!(event instanceof Map<?, ?> eventMap)) {
            return true;
        }
        Object value = eventMap.get(eventKey);
        return !(value instanceof Boolean) || (Boolean) value;
    }

    private Long resolveSenderId() {
        LoginUser loginUser = authContext.getLoginUser();
        return loginUser != null ? loginUser.getUserId() : null;
    }

    private static String buildTitle(SysDefect defect) {
        String stateLabel = resolveStateLabel(defect.getDefectState());
        return String.format("[%s]缺陷:#%d %s",
                stateLabel,
                defect.getProjectNum() != null ? defect.getProjectNum() : 0,
                blankToEmpty(defect.getDefectName()));
    }

    private static String resolveStateLabel(Integer defectState) {
        if (defectState == null) {
            return "";
        }
        for (EnumVo state : DefectConstants.getDefectStateList()) {
            if (state.getKey() != null && state.getKey().intValue() == defectState) {
                return state.getValue() != null ? state.getValue() : String.valueOf(defectState);
            }
        }
        if (defectState >= 0 && defectState < DefectConstants.DEFECT_STATES.length) {
            return DefectConstants.DEFECT_STATES[defectState];
        }
        return String.valueOf(defectState);
    }

    private static String buildMarkdownContent(SysDefect defect, String srcHost) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("## #%d %s \n\n",
                defect.getProjectNum() != null ? defect.getProjectNum() : 0,
                blankToEmpty(defect.getDefectName())));
        String link = buildLink(defect, srcHost);
        if (StringUtils.isNotEmpty(link)) {
            sb.append(String.format("[查看缺陷详情](%s) \n\n", link));
        }
        if (StringUtils.isNotEmpty(defect.getDefectDescribe())) {
            sb.append("**描述:** ").append(defect.getDefectDescribe()).append(" \n\n");
        }
        if (defect.getHandleByList() != null && !defect.getHandleByList().isEmpty()) {
            sb.append("**处理人:** ").append(formatHandleByNames(defect)).append(" \n\n");
        }
        return sb.toString();
    }

    private static String formatHandleByNames(SysDefect defect) {
        return defect.getHandleByList().stream()
                .map(user -> {
                    if (user == null) {
                        return "";
                    }
                    if (StringUtils.isNotEmpty(user.getNickName())) {
                        return user.getNickName();
                    }
                    return blankToEmpty(user.getUserName());
                })
                .filter(StringUtils::isNotEmpty)
                .reduce((left, right) -> left + ", " + right)
                .orElse("");
    }

    private static String buildLink(SysDefect defect, String srcHost) {
        if (StringUtils.isEmpty(srcHost) || defect.getDefectId() == null) {
            return "";
        }
        String host = srcHost.endsWith("/") ? srcHost.substring(0, srcHost.length() - 1) : srcHost;
        if (defect.getProjectId() != null) {
            return String.format("%s/#/project/defect?projectId=%d&defectId=%d",
                    host, defect.getProjectId(), defect.getDefectId());
        }
        return String.format("%s/#/project/defect?defectId=%d", host, defect.getDefectId());
    }

    private static String blankToEmpty(String value) {
        return value != null ? value : "";
    }
}
