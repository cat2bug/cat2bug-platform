package com.cat2bug.system.util;

import com.cat2bug.common.core.domain.entity.SysDefect;
import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.system.domain.SysCase;
import com.cat2bug.system.domain.SysModule;
import com.cat2bug.system.mapper.SysCaseMapper;
import com.cat2bug.system.mapper.SysModuleMapper;
import com.cat2bug.system.mapper.SysUserMapper;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 缺陷字段差异计算工具。
 * <p>负责对比"编辑前/编辑后"两份 SysDefect 对象，按业务白名单产出 {@link DefectChange} 列表。</p>
 * <p>状态相关字段（defectState、createBy 等）由专用日志类型记录，故不在白名单内。</p>
 */
public final class DefectChangeUtil {

    /** 写入日志时描述快照最大字符数（完整比对仍用原始字符串，仅持久化截断） */
    private static final int MAX_DEFECT_DESCRIBE_LOG_CHARS = 4000;

    /** SimpleDateFormat 非线程安全，并发编辑缺陷时会在计划时间等字段上随机抛异常，导致整条 UPDATE 日志丢失 */
    private static final DateTimeFormatter LOG_DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private DefectChangeUtil() {
    }

    private static String formatLogDate(Date date) {
        if (date == null) {
            return null;
        }
        return LOG_DATE_FMT.format(date.toInstant());
    }

    /**
     * 仅比较基础字段，不解析任何展示文本。
     */
    public static List<DefectChange> diff(SysDefect oldDefect, SysDefect newDefect) {
        return diff(oldDefect, newDefect, null, null, null);
    }

    /**
     * 比较两个 SysDefect，并对 handleBy / moduleId / caseId 解析展示文本。
     *
     * @param userMapper   可空，传入则解析 handleBy 的昵称
     * @param moduleMapper 可空，传入则解析 moduleId 的模块名
     * @param caseMapper   可空，传入则解析 caseId 的用例名
     */
    public static List<DefectChange> diff(SysDefect oldDefect, SysDefect newDefect,
                                          SysUserMapper userMapper,
                                          SysModuleMapper moduleMapper,
                                          SysCaseMapper caseMapper) {
        List<DefectChange> changes = new ArrayList<>();
        if (oldDefect == null || newDefect == null) {
            return changes;
        }

        addTextChange(changes, "defectName", oldDefect.getDefectName(), newDefect.getDefectName());
        addEnumChange(changes, "defectType",
                oldDefect.getDefectType() == null ? null : oldDefect.getDefectType().name(),
                newDefect.getDefectType() == null ? null : newDefect.getDefectType().name());
        addEnumChange(changes, "defectLevel", oldDefect.getDefectLevel(), newDefect.getDefectLevel());

        addUsersChange(changes, "handleBy", oldDefect.getHandleBy(), newDefect.getHandleBy(),
                oldDefect.getProjectId(), userMapper);

        addModuleChange(changes, oldDefect.getModuleId(), newDefect.getModuleId(), moduleMapper);
        addTextChange(changes, "moduleVersion", oldDefect.getModuleVersion(), newDefect.getModuleVersion());

        addTimeChange(changes, "planStartTime", oldDefect.getPlanStartTime(), newDefect.getPlanStartTime());
        addTimeChange(changes, "planEndTime", oldDefect.getPlanEndTime(), newDefect.getPlanEndTime());

        addCaseChange(changes, oldDefect.getCaseId(), newDefect.getCaseId(), caseMapper);
        addNumberChange(changes, "caseStepId", oldDefect.getCaseStepId(), newDefect.getCaseStepId());

        addDefectDescribeChange(changes, oldDefect.getDefectDescribe(), newDefect.getDefectDescribe());

        addUrlListChange(changes, "imgUrls", "images", oldDefect.getImgUrls(), newDefect.getImgUrls());
        addUrlListChange(changes, "annexUrls", "files", oldDefect.getAnnexUrls(), newDefect.getAnnexUrls());

        return changes;
    }

    private static void addTextChange(List<DefectChange> changes, String field, String oldValue, String newValue) {
        String o = nullToEmpty(oldValue);
        String n = nullToEmpty(newValue);
        if (!o.equals(n)) {
            changes.add(new DefectChange(field, "text", oldValue, newValue));
        }
    }

    /**
     * 描述可能很长；写入日志时截断快照，避免超大 JSON 或旧库 varchar(255) 插入失败。
     */
    private static void addDefectDescribeChange(List<DefectChange> changes, String oldValue, String newValue) {
        String o = nullToEmpty(oldValue);
        String n = nullToEmpty(newValue);
        if (!o.equals(n)) {
            changes.add(new DefectChange("defectDescribe", "text",
                    truncateForLog(oldValue, MAX_DEFECT_DESCRIBE_LOG_CHARS),
                    truncateForLog(newValue, MAX_DEFECT_DESCRIBE_LOG_CHARS)));
        }
    }

    private static String truncateForLog(String s, int maxChars) {
        if (s == null) {
            return null;
        }
        if (s.length() <= maxChars) {
            return s;
        }
        return s.substring(0, maxChars) + "...";
    }

    private static void addEnumChange(List<DefectChange> changes, String field, String oldValue, String newValue) {
        String o = nullToEmpty(oldValue);
        String n = nullToEmpty(newValue);
        if (!o.equals(n)) {
            DefectChange c = new DefectChange(field, "enum", oldValue, newValue);
            c.setOldDisplay(oldValue);
            c.setNewDisplay(newValue);
            changes.add(c);
        }
    }

    private static void addNumberChange(List<DefectChange> changes, String field, Number oldValue, Number newValue) {
        if (!Objects.equals(oldValue, newValue)) {
            changes.add(new DefectChange(field, "number", oldValue, newValue));
        }
    }

    private static void addTimeChange(List<DefectChange> changes, String field, Date oldValue, Date newValue) {
        Long o = oldValue == null ? null : oldValue.getTime();
        Long n = newValue == null ? null : newValue.getTime();
        if (!Objects.equals(o, n)) {
            DefectChange c = new DefectChange(field, "time", o, n);
            c.setOldDisplay(formatLogDate(oldValue));
            c.setNewDisplay(formatLogDate(newValue));
            changes.add(c);
        }
    }

    private static void addUsersChange(List<DefectChange> changes, String field,
                                       List<Long> oldIds, List<Long> newIds,
                                       Long projectId, SysUserMapper userMapper) {
        List<Long> o = oldIds == null ? Collections.emptyList() : oldIds;
        List<Long> n = newIds == null ? Collections.emptyList() : newIds;
        if (sameOrderedIds(o, n)) {
            return;
        }
        DefectChange c = new DefectChange(field, "users", o, n);
        if (userMapper != null) {
            c.setOldDisplay(joinUserNicknames(o, projectId, userMapper));
            c.setNewDisplay(joinUserNicknames(n, projectId, userMapper));
        }
        changes.add(c);
    }

    private static void addModuleChange(List<DefectChange> changes, Long oldId, Long newId, SysModuleMapper moduleMapper) {
        if (Objects.equals(oldId, newId)) {
            return;
        }
        DefectChange c = new DefectChange("moduleId", "module", oldId, newId);
        if (moduleMapper != null) {
            c.setOldDisplay(resolveModuleName(oldId, moduleMapper));
            c.setNewDisplay(resolveModuleName(newId, moduleMapper));
        }
        changes.add(c);
    }

    private static void addCaseChange(List<DefectChange> changes, Long oldId, Long newId, SysCaseMapper caseMapper) {
        if (Objects.equals(oldId, newId)) {
            return;
        }
        DefectChange c = new DefectChange("caseId", "case", oldId, newId);
        if (caseMapper != null) {
            c.setOldDisplay(resolveCaseName(oldId, caseMapper));
            c.setNewDisplay(resolveCaseName(newId, caseMapper));
        }
        changes.add(c);
    }

    /**
     * 处理 imgUrls / annexUrls：仅记录数量差，不写入完整 URL，避免日志过大。
     */
    private static void addUrlListChange(List<DefectChange> changes, String field, String type,
                                         String oldUrls, String newUrls) {
        Set<String> o = splitUrls(oldUrls);
        Set<String> n = splitUrls(newUrls);
        if (o.equals(n)) {
            return;
        }
        Set<String> added = new HashSet<>(n);
        added.removeAll(o);
        Set<String> removed = new HashSet<>(o);
        removed.removeAll(n);
        DefectChange c = new DefectChange(field, type, o.size(), n.size());
        c.setUrlAddedCount(added.size());
        c.setUrlRemovedCount(removed.size());
        changes.add(c);
    }

    private static Set<String> splitUrls(String urls) {
        if (StringUtils.isBlank(urls)) {
            return Collections.emptySet();
        }
        return Arrays.stream(urls.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(HashSet::new));
    }

    private static boolean sameOrderedIds(List<Long> a, List<Long> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (!Objects.equals(a.get(i), b.get(i))) {
                return false;
            }
        }
        return true;
    }

    private static String joinUserNicknames(List<Long> ids, Long projectId, SysUserMapper userMapper) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        List<String> names = new ArrayList<>();
        for (Long id : ids) {
            if (id == null) {
                continue;
            }
            try {
                SysUser u = userMapper.selectUserById(null, projectId, id);
                names.add(u != null && StringUtils.isNotBlank(u.getNickName()) ? u.getNickName() : String.valueOf(id));
            } catch (Exception e) {
                names.add(String.valueOf(id));
            }
        }
        return String.join(",", names);
    }

    private static String resolveModuleName(Long moduleId, SysModuleMapper mapper) {
        if (moduleId == null) {
            return null;
        }
        try {
            SysModule m = mapper.selectSysModuleByModuleId(moduleId);
            if (m == null) {
                return String.valueOf(moduleId);
            }
            return StringUtils.isNotBlank(m.getModulePath()) ? m.getModulePath() : m.getModuleName();
        } catch (Exception e) {
            return String.valueOf(moduleId);
        }
    }

    private static String resolveCaseName(Long caseId, SysCaseMapper mapper) {
        if (caseId == null) {
            return null;
        }
        try {
            SysCase c = mapper.selectSysCaseByCaseId(caseId);
            return c == null ? String.valueOf(caseId) : c.getCaseName();
        } catch (Exception e) {
            return String.valueOf(caseId);
        }
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
