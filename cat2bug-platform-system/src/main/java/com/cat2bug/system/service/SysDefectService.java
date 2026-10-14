package com.cat2bug.system.service;

import com.cat2bug.common.api.TableDataInfo;
import com.cat2bug.common.constant.DefectConstants;
import com.cat2bug.common.constant.DefectLogState;
import com.cat2bug.common.core.page.PageDomain;
import com.cat2bug.common.domain.ImportDefectResult;
import com.cat2bug.common.domain.LoginUser;
import com.cat2bug.common.domain.SysDefect;
import com.cat2bug.common.domain.SysDefectLog;
import com.cat2bug.common.domain.SysModule;
import com.cat2bug.common.domain.SysProjectDefectTabs;
import com.cat2bug.common.domain.SysUser;
import com.cat2bug.common.domain.SysUserConfig;
import com.cat2bug.common.domain.SysVersion;
import com.cat2bug.common.domain.vo.EnumVo;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.common.util.DefectDefaults;
import com.cat2bug.common.util.StringUtils;
import com.cat2bug.framework.security.AuthContext;
import com.cat2bug.system.mapper.SysDefectLogMapper;
import com.cat2bug.system.mapper.SysDefectMapper;
import com.cat2bug.system.mapper.SysModuleMapper;
import com.cat2bug.system.mapper.SysUserProjectMapper;
import com.cat2bug.system.util.DefectCustomFieldsValidator;
import com.cat2bug.system.util.DefectImportLabelResolver;
import com.cat2bug.system.util.DefectMemberEnricher;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class SysDefectService {

    @Inject
    SysDefectMapper sysDefectMapper;

    @Inject
    SysDefectLogMapper sysDefectLogMapper;

    @Inject
    AuthContext authContext;

    @Inject
    SysUserConfigService sysUserConfigService;

    @Inject
    SysProjectDefectTabsService sysProjectDefectTabsService;

    @Inject
    DefectMemberEnricher defectMemberEnricher;

    @Inject
    MemberFocusService memberFocusService;

    @Inject
    SysModuleMapper sysModuleMapper;

    @Inject
    SysUserProjectMapper sysUserProjectMapper;

    @Inject
    DefectCustomFieldsValidator defectCustomFieldsValidator;

    @Inject
    SysDictDataService sysDictDataService;

    public Map<String, Object> getDefectConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("types", DefectConstants.getDefectTypeList());
        config.put("states", DefectConstants.getDefectStateList());
        config.put("tabs", loadDefectTabsForCurrentUser());
        return config;
    }

    private List<SysProjectDefectTabs> loadDefectTabsForCurrentUser() {
        LoginUser loginUser = authContext.getLoginUser();
        if (loginUser == null) {
            return List.of();
        }
        SysUserConfig userConfig = sysUserConfigService.selectSysUserConfigByUserId(loginUser.getUserId());
        if (userConfig == null || userConfig.getCurrentProjectId() == null) {
            return List.of();
        }
        SysProjectDefectTabs query = new SysProjectDefectTabs();
        query.setProjectId(userConfig.getCurrentProjectId());
        query.setUserId(loginUser.getUserId());
        return sysProjectDefectTabsService.selectSysProjectDefectTabsList(query);
    }

    public List<EnumVo> getDefectTypeList() {
        return DefectConstants.getDefectTypeList();
    }

    public List<EnumVo> getDefectStateList() {
        return DefectConstants.getDefectStateList();
    }

    public TableDataInfo selectDefectPage(SysDefect query, Integer pageNum, Integer pageSize) {
        PageDomain page = PageDomain.of(pageNum, pageSize);
        long total = sysDefectMapper.countSysDefectList(query);
        var rows = sysDefectMapper.selectSysDefectList(query, page.getOffset(), page.getPageSize());
        defectMemberEnricher.enrich(rows);
        return new TableDataInfo(rows, total);
    }

    public List<SysDefect> selectAllSysDefectList(SysDefect query) {
        List<SysDefect> rows = sysDefectMapper.selectAllSysDefectList(query);
        defectMemberEnricher.enrich(rows);
        return rows;
    }

    @Transactional
    public ImportDefectResult importDefect(Long projectId, List<SysDefect> list, boolean updateSupport) {
        if (list == null || list.isEmpty()) {
            throw new ApiException("导入数据不能为空");
        }
        List<String> errors = new ArrayList<>();
        Long maxNum = sysDefectMapper.getProjectDefectMaxNum(projectId);
        long count = maxNum != null ? maxNum : 0L;

        Map<String, SysModule> moduleMap = sysModuleMapper.selectSysModulePathList(projectId).stream()
                .filter(module -> module.getModulePath() != null)
                .collect(Collectors.toMap(SysModule::getModulePath, module -> module, (left, right) -> left));
        Map<String, SysUser> userMap = sysUserProjectMapper
                .selectSysUserListByProjectId(projectId, null, 0, Integer.MAX_VALUE).stream()
                .filter(user -> user.getNickName() != null)
                .collect(Collectors.toMap(SysUser::getNickName, user -> user, (left, right) -> left));
        var levelDict = sysDictDataService.selectDictDataByType("defect_level");

        List<SysDefect> inserts = new ArrayList<>();

        for (int i = 0; i < list.size(); i++) {
            SysDefect defect = list.get(i);
            int line = i + 2;
            if (defect == null) {
                errors.add("第" + line + "行数据格式异常");
                continue;
            }
            List<String> emptyCells = new ArrayList<>();
            List<String> invalidCells = new ArrayList<>();

            if (StringUtils.isEmpty(defect.getDefectName())) {
                emptyCells.add("缺陷名称");
            }
            if (StringUtils.isNotEmpty(defect.getDefectStateImportName())) {
                Integer state = DefectImportLabelResolver.resolveState(defect.getDefectStateImportName());
                if (state != null) {
                    defect.setDefectState(state);
                } else {
                    invalidCells.add("缺陷状态");
                }
            }
            if (StringUtils.isNotEmpty(defect.getDefectTypeImportName())) {
                Integer type = DefectImportLabelResolver.resolveType(defect.getDefectTypeImportName());
                if (type != null) {
                    defect.setDefectType(type);
                } else {
                    invalidCells.add("类型");
                }
            }
            if (StringUtils.isNotEmpty(defect.getDefectLevel())) {
                String level = DefectImportLabelResolver.resolveLevel(defect.getDefectLevel(), levelDict);
                if (level != null) {
                    defect.setDefectLevel(level);
                } else {
                    invalidCells.add("缺陷等级");
                }
            }
            String moduleLabel = StringUtils.isNotEmpty(defect.getModulePath())
                    ? defect.getModulePath() : defect.getModuleName();
            if (StringUtils.isNotEmpty(moduleLabel)) {
                SysModule module = moduleMap.get(moduleLabel);
                if (module != null) {
                    defect.setModuleId(module.getModuleId());
                    defect.setModuleName(module.getModulePath());
                } else {
                    invalidCells.add("交付物");
                }
            }
            if (StringUtils.isNotEmpty(defect.getHandleByNames())) {
                SysUser user = userMap.get(defect.getHandleByNames());
                if (user != null) {
                    defect.setHandleBy(Collections.singletonList(user.getUserId()));
                } else {
                    invalidCells.add("处理人");
                }
            } else {
                emptyCells.add("处理人");
            }

            if (!emptyCells.isEmpty()) {
                errors.add("第" + line + "行数据不能为空：" + String.join("、", emptyCells));
            }
            if (!invalidCells.isEmpty()) {
                errors.add("第" + line + "行数据无效：" + String.join("、", invalidCells));
            }
            try {
                defect.setCustomFields(defectCustomFieldsValidator.validateAndStrip(
                        projectId, defect.getCustomFields() != null ? defect.getCustomFields() : new HashMap<>()));
            } catch (ApiException ex) {
                errors.add("第" + line + "行数据无效：" + ex.getMessage());
            }

            boolean rowHasError = errors.stream().anyMatch(msg -> msg.startsWith("第" + line + "行"));
            if (rowHasError) {
                continue;
            }

            LoginUser loginUser = requireLoginUser();
            Date now = new Date();
            defect.setProjectId(projectId);
            defect.setDelFlag("0");
            defect.setUpdateTime(now);
            defect.setUpdateBy(loginUser.getUsername());
            defect.setUpdateById(loginUser.getUserId());

            if (updateSupport && defect.getProjectNum() != null) {
                SysDefect existing = sysDefectMapper.selectByProjectIdAndProjectNum(projectId, defect.getProjectNum());
                if (existing != null) {
                    defect.setDefectId(existing.getDefectId());
                    DefectDefaults.applyDefectDefaults(defect);
                    if (sysDefectMapper.updateSysDefect(defect) <= 0) {
                        errors.add("第" + line + "行更新失败");
                    }
                    continue;
                }
            }

            defect.setProjectNum(++count);
            defect.setCreateTime(now);
            defect.setCreateBy(loginUser.getUsername());
            defect.setCreateById(loginUser.getUserId());
            inserts.add(defect);
        }

        if (!errors.isEmpty()) {
            return ImportDefectResult.failure(String.join("<br/>", errors));
        }

        inserts.forEach(DefectDefaults::applyDefectDefaults);
        int batchSize = 50;
        for (int start = 0; start < inserts.size(); start += batchSize) {
            int end = Math.min(start + batchSize, inserts.size());
            List<SysDefect> batch = inserts.subList(start, end);
            sysDefectMapper.batchInsertSysDefect(batch);
            batchInsertImportLogs(batch);
        }
        List<Long> insertedIds = inserts.stream()
                .map(SysDefect::getDefectId)
                .filter(id -> id != null)
                .toList();
        return ImportDefectResult.success(insertedIds);
    }

    public SysDefect selectSysDefectByDefectId(Long defectId) {
        SysDefect defect = selectSysDefectByDefectIdOptional(defectId);
        if (defect == null) {
            throw new ApiException("缺陷不存在");
        }
        return defect;
    }

    public SysDefect selectSysDefectByDefectIdOptional(Long defectId) {
        if (defectId == null) {
            return null;
        }
        SysDefect defect = sysDefectMapper.selectSysDefectByDefectId(defectId);
        defectMemberEnricher.enrich(defect);
        return defect;
    }

    public SysDefect insertSysDefect(SysDefect defect) {
        LoginUser loginUser = requireLoginUser();
        Date now = new Date();
        DefectDefaults.applyDefectDefaults(defect);
        defect.setCreateTime(now);
        defect.setUpdateTime(now);
        defect.setCreateBy(loginUser.getUsername());
        defect.setUpdateBy(loginUser.getUsername());
        defect.setCreateById(loginUser.getUserId());
        defect.setUpdateById(loginUser.getUserId());
        defect.setDelFlag("0");
        Long maxNum = sysDefectMapper.getProjectDefectMaxNum(defect.getProjectId());
        defect.setProjectNum(maxNum == null ? 1L : maxNum + 1);
        if (sysDefectMapper.insertSysDefect(defect) <= 0) {
            throw new ApiException("新增缺陷失败");
        }
        defectMemberEnricher.enrich(defect);
        return defect;
    }

    public int updateSysDefect(SysDefect defect) {
        if (defect.getDefectId() == null) {
            throw new ApiException("缺陷ID不能为空");
        }
        selectSysDefectByDefectId(defect.getDefectId());
        LoginUser loginUser = requireLoginUser();
        DefectDefaults.applyDefectDefaults(defect);
        defect.setUpdateTime(new Date());
        defect.setUpdateBy(loginUser.getUsername());
        defect.setUpdateById(loginUser.getUserId());
        return sysDefectMapper.updateSysDefect(defect);
    }

    public int deleteSysDefectByDefectId(Long defectId) {
        selectSysDefectByDefectId(defectId);
        return sysDefectMapper.deleteSysDefectByDefectId(defectId);
    }

    @Transactional
    public int restoreSysDefectByDefectId(Long defectId) {
        LoginUser loginUser = requireLoginUser();
        Date now = new Date();
        SysDefect patch = new SysDefect();
        patch.setDefectId(defectId);
        patch.setUpdateTime(now);
        patch.setUpdateBy(loginUser.getUsername());
        patch.setUpdateById(loginUser.getUserId());
        int ret = sysDefectMapper.restoreSysDefectByDefectId(patch);
        if (ret > 0) {
            SysDefectLog log = new SysDefectLog();
            log.setDefectId(defectId);
            log.setDefectLogType(DefectLogState.RESTORE);
            insertLog(log);
        }
        return ret;
    }

    @Transactional
    public SysDefectLog assign(SysDefectLog log) {
        SysDefect patch = new SysDefect();
        patch.setDefectId(requireDefectId(log));
        patch.setHandleBy(log.getReceiveBy());
        doUpdateSysDefect(patch);

        log.setDefectLogType(DefectLogState.ASSIGN);
        insertLog(log);
        return sysDefectLogMapper.selectSysDefectLogByDefectLogId(log.getDefectLogId());
    }

    @Transactional
    public SysDefectLog reject(SysDefectLog log) {
        SysDefect existing = selectSysDefectByDefectId(requireDefectId(log));
        existing.setDefectState(DefectConstants.STATE_REJECTED);
        existing.setHandleBy(resolveRejectHandleBy(existing));
        doUpdateSysDefect(existing);

        log.setDefectLogType(DefectLogState.REJECTED);
        log.setReceiveBy(existing.getHandleBy());
        insertLog(log);
        return sysDefectLogMapper.selectSysDefectLogByDefectLogId(log.getDefectLogId());
    }

    @Transactional
    public SysDefectLog repair(SysDefectLog log) {
        SysDefect patch = new SysDefect();
        patch.setDefectId(requireDefectId(log));
        patch.setDefectState(DefectConstants.STATE_AUDIT);
        patch.setHandleBy(log.getReceiveBy());
        doUpdateSysDefect(patch);

        log.setDefectLogType(DefectLogState.REPAIR);
        log.setReceiveBy(patch.getHandleBy());
        insertLog(log);
        return sysDefectLogMapper.selectSysDefectLogByDefectLogId(log.getDefectLogId());
    }

    @Transactional
    public SysDefectLog pass(SysDefectLog log) {
        SysDefect patch = new SysDefect();
        patch.setDefectId(requireDefectId(log));
        patch.setDefectState(DefectConstants.STATE_CLOSED);
        doUpdateSysDefect(patch);

        log.setDefectLogType(DefectLogState.PASS);
        insertLog(log);
        return sysDefectLogMapper.selectSysDefectLogByDefectLogId(log.getDefectLogId());
    }

    @Transactional
    public SysDefectLog close(SysDefectLog log) {
        SysDefect patch = new SysDefect();
        patch.setDefectId(requireDefectId(log));
        patch.setDefectState(DefectConstants.STATE_CLOSED);
        doUpdateSysDefect(patch);

        log.setDefectLogType(DefectLogState.CLOSED);
        insertLog(log);
        return sysDefectLogMapper.selectSysDefectLogByDefectLogId(log.getDefectLogId());
    }

    @Transactional
    public SysDefectLog open(SysDefectLog log) {
        SysDefect patch = new SysDefect();
        patch.setDefectId(requireDefectId(log));
        patch.setDefectState(DefectConstants.STATE_PROCESSING);
        patch.setHandleBy(log.getReceiveBy());
        doUpdateSysDefect(patch);

        log.setDefectLogType(DefectLogState.OPEN);
        insertLog(log);
        return sysDefectLogMapper.selectSysDefectLogByDefectLogId(log.getDefectLogId());
    }

    /** 关闭前端编辑窗口（清除成员编辑焦点）。 */
    public void closeEditWindow(Long defectId) {
        LoginUser loginUser = authContext.getLoginUser();
        if (loginUser != null) {
            memberFocusService.removeFocus(loginUser.getUserId());
        }
    }

    private int doUpdateSysDefect(SysDefect defect) {
        if (defect.getDefectId() == null) {
            throw new ApiException("缺陷ID不能为空");
        }
        selectSysDefectByDefectId(defect.getDefectId());
        LoginUser loginUser = requireLoginUser();
        defect.setUpdateTime(new Date());
        defect.setUpdateBy(String.valueOf(loginUser.getUserId()));
        defect.setUpdateById(loginUser.getUserId());
        int rows = sysDefectMapper.updateSysDefect(defect);
        if (rows <= 0) {
            throw new ApiException("更新缺陷失败");
        }
        return rows;
    }

    private void batchInsertImportLogs(List<SysDefect> defects) {
        if (defects == null || defects.isEmpty()) {
            return;
        }
        LoginUser loginUser = requireLoginUser();
        Date now = new Date();
        List<SysDefectLog> logs = new ArrayList<>();
        for (SysDefect defect : defects) {
            SysDefectLog log = new SysDefectLog();
            log.setDefectId(defect.getDefectId());
            log.setReceiveBy(defect.getHandleBy());
            log.setDefectLogType(DefectLogState.IMPORT);
            log.setCreateBy(String.valueOf(loginUser.getUserId()));
            log.setCreateTime(now);
            logs.add(log);
        }
        sysDefectLogMapper.batchInsertSysDefectLog(logs);
    }

    private void insertLog(SysDefectLog log) {
        if (log.getDefectId() == null) {
            throw new ApiException("缺陷ID不能为空");
        }
        LoginUser loginUser = requireLoginUser();
        log.setCreateBy(String.valueOf(loginUser.getUserId()));
        log.setCreateTime(new Date());
        if (sysDefectLogMapper.insertSysDefectLog(log) <= 0) {
            throw new ApiException("写入缺陷日志失败");
        }
    }

    private Long requireDefectId(SysDefectLog log) {
        if (log.getDefectId() == null) {
            throw new ApiException("缺陷ID不能为空");
        }
        return log.getDefectId();
    }

    private List<Long> resolveRejectHandleBy(SysDefect defect) {
        if (defect.getUpdateById() != null) {
            return Collections.singletonList(defect.getUpdateById());
        }
        if (defect.getUpdateBy() != null && !defect.getUpdateBy().isBlank()) {
            try {
                return Collections.singletonList(Long.valueOf(defect.getUpdateBy()));
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }
        return Collections.emptyList();
    }

    private LoginUser requireLoginUser() {
        LoginUser loginUser = authContext.getLoginUser();
        if (loginUser == null) {
            throw new ApiException(401, "未登录或会话已失效");
        }
        return loginUser;
    }

    public List<SysVersion> selectVersionList(Long projectId) {
        if (projectId == null) {
            return Collections.emptyList();
        }
        return sysDefectMapper.selectVersionList(projectId);
    }
}
