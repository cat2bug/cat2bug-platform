package com.cat2bug.system.open;

import com.cat2bug.common.api.TableDataInfo;
import com.cat2bug.common.core.page.PageDomain;
import com.cat2bug.common.domain.LoginUser;
import com.cat2bug.common.domain.SysUser;
import com.cat2bug.common.domain.open.ApiDefect;
import com.cat2bug.common.domain.open.ApiDefectHandle;
import com.cat2bug.common.domain.open.ApiDefectLog;
import com.cat2bug.common.domain.open.ApiDefectRequest;
import com.cat2bug.common.domain.open.ApiDeliverable;
import com.cat2bug.common.domain.open.ApiMember;
import com.cat2bug.common.domain.open.ApiMemberBaseInfo;
import com.cat2bug.common.domain.open.type.ApiDefectLogStateEnum;
import com.cat2bug.common.domain.open.type.ApiDefectStateEnum;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.common.util.StringUtils;
import com.cat2bug.framework.security.AuthContext;
import com.cat2bug.system.mapper.OpenApiDefectLogMapper;
import com.cat2bug.system.mapper.OpenApiDefectMapper;
import com.cat2bug.system.mapper.OpenApiMemberMapper;
import com.cat2bug.system.mapper.SysUserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@ApplicationScoped
public class OpenApiDefectService {

    private static final Logger LOG = Logger.getLogger(OpenApiDefectService.class);
    private static final long DEFECT_DESCRIBE_MAX_LEN = 65536;
    private static final long DEFECT_NAME_MAX_LEN = 128;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Inject
    AuthContext authContext;

    @Inject
    OpenApiDefectMapper openApiDefectMapper;

    @Inject
    OpenApiDefectLogMapper openApiDefectLogMapper;

    @Inject
    OpenApiMemberMapper openApiMemberMapper;

    @Inject
    SysUserMapper sysUserMapper;

    @Inject
    OpenApiDeliverableService openApiDeliverableService;

    public TableDataInfo selectApiDefectPage(Long projectId, ApiDefectRequest apiDefect,
                                             Integer pageNum, Integer pageSize) {
        ApiDefectRequest filter = normalizeDefectLevelFilter(apiDefect);
        PageDomain page = PageDomain.of(pageNum, pageSize);
        Map<Long, ApiDeliverable> pathMap = deliverablePathMap(projectId);
        long total = openApiDefectMapper.countApiDefectList(projectId, filter);
        List<ApiDefect> rows = openApiDefectMapper.selectApiDefectPage(
                        projectId, filter, page.getOffset(), page.getPageSize()).stream()
                .map(d -> enrichDefect(d, pathMap))
                .collect(Collectors.toList());
        return new TableDataInfo(rows, total);
    }

    public List<ApiDefect> selectApiDefectList(Long projectId, ApiDefectRequest apiDefect) {
        ApiDefectRequest filter = normalizeDefectLevelFilter(apiDefect);
        Map<Long, ApiDeliverable> pathMap = deliverablePathMap(projectId);
        return openApiDefectMapper.selectApiDefectList(projectId, filter).stream()
                .map(d -> enrichDefect(d, pathMap))
                .collect(Collectors.toList());
    }

    public ApiDefect selectSysDefectByDefectNumber(Long projectId, Long number) {
        ApiDefect apiDefect = openApiDefectMapper.selectSysDefectByDefectNumber(projectId, number);
        if (apiDefect == null) {
            return null;
        }
        Map<Long, ApiDeliverable> pathMap = deliverablePathMap(projectId);
        return enrichDefect(apiDefect, pathMap);
    }

    @Transactional
    public ApiDefect insertApiDefect(Long projectId, ApiDefectRequest apiDefect) {
        if (apiDefect == null) {
            throw new ApiException("缺陷名称不能为空");
        }
        if (apiDefect.getDefectName() == null) {
            throw new ApiException("缺陷名称不能为空");
        }
        if (apiDefect.getDefectType() == null) {
            throw new ApiException("缺陷类型不能为空");
        }
        if (apiDefect.getDefectLevel() == null) {
            throw new ApiException("缺陷等级不能为空");
        }
        if (apiDefect.getDefectName().length() > DEFECT_NAME_MAX_LEN) {
            throw new ApiException("缺陷名称长度不能超过" + DEFECT_NAME_MAX_LEN);
        }
        String describe = apiDefect.getDefectDescribe() == null ? "" : apiDefect.getDefectDescribe();
        if (describe.length() > DEFECT_DESCRIBE_MAX_LEN) {
            throw new ApiException("缺陷描述长度不能超过" + DEFECT_DESCRIBE_MAX_LEN);
        }
        apiDefect.setDefectDescribe(describe);
        if (apiDefect.getHandlerAccountList() == null || apiDefect.getHandlerAccountList().isEmpty()) {
            throw new ApiException("处理人不能为空");
        }

        if (StringUtils.isNotBlank(apiDefect.getDefectLevel())) {
            apiDefect.setDefectLevel(apiDefect.getDefectLevel().toLowerCase());
        }

        LoginUser loginUser = requireLoginUser();
        Date now = new Date();
        apiDefect.setDefectState(ApiDefectStateEnum.PROCESSING);
        apiDefect.setUpdateTime(now);
        apiDefect.setUpdateBy(loginUser.getUsername());
        apiDefect.setUpdateById(loginUser.getUserId());
        resolveHandlers(projectId, apiDefect);

        apiDefect.setCreateTime(now);
        apiDefect.setCreateBy(loginUser.getUsername());
        apiDefect.setCreateById(loginUser.getUserId());

        long count = openApiDefectMapper.getProjectDefectMaxNum(projectId);
        apiDefect.setDefectNum(++count);
        if (openApiDefectMapper.insertApiDefect(projectId, apiDefect) <= 0) {
            throw new ApiException("新增缺陷失败");
        }
        insertLog(apiDefect.getDefectId(), apiDefect.getHandleBy(),
                apiDefect.getDefectDescribe() != null ? apiDefect.getDefectDescribe() : "",
                ApiDefectLogStateEnum.CREATE);

        ApiDefect retDefect = openApiDefectMapper.selectSysDefectByDefectNumber(projectId, count);
        return enrichDefect(retDefect, deliverablePathMap(projectId));
    }

    @Transactional
    public ApiDefect updateSysDefect(Long projectId, ApiDefectRequest apiDefect) {
        if (apiDefect == null) {
            apiDefect = new ApiDefectRequest();
        }
        if (apiDefect.getDefectNum() == null) {
            throw new ApiException("缺陷编号不能为空");
        }
        ApiDefect oldDefect = openApiDefectMapper.selectSysDefectByDefectNumber(projectId, apiDefect.getDefectNum());
        if (oldDefect == null) {
            throw new ApiException("缺陷不存在");
        }

        LoginUser loginUser = requireLoginUser();
        apiDefect.setDefectId(oldDefect.getDefectId());
        apiDefect.setUpdateTime(new Date());
        apiDefect.setUpdateBy(loginUser.getUsername());
        apiDefect.setUpdateById(loginUser.getUserId());
        apiDefect.setProjectId(projectId);

        int ret = openApiDefectMapper.updateApiDefect(apiDefect);
        if (ret > 0) {
            try {
                List<Map<String, Object>> changes = buildApiDefectChanges(oldDefect, apiDefect);
                if (!changes.isEmpty()) {
                    insertLog(oldDefect.getDefectId(), apiDefect.getHandleBy(),
                            MAPPER.writeValueAsString(changes), ApiDefectLogStateEnum.UPDATE);
                }
            } catch (Exception e) {
                LOG.error("write api defect update log failed, defectNum=" + apiDefect.getDefectNum(), e);
            }
            return selectSysDefectByDefectNumber(projectId, apiDefect.getDefectNum());
        }
        return null;
    }

    @Transactional
    public ApiDefect assign(Long projectId, ApiDefectHandle apiDefectHandle) {
        if (apiDefectHandle.getHandlerAccountList() == null || apiDefectHandle.getHandlerAccountList().isEmpty()) {
            throw new ApiException("处理人不能为空");
        }
        return updateDefectHandle(projectId, apiDefectHandle, null, ApiDefectLogStateEnum.ASSIGN, "指派失败");
    }

    public ApiDefect reject(Long projectId, ApiDefectHandle apiDefectHandle) {
        if (apiDefectHandle.getDescribe() == null) {
            throw new ApiException("缺陷描述不能为空");
        }
        return updateDefectHandle(projectId, apiDefectHandle, ApiDefectStateEnum.REJECTED,
                ApiDefectLogStateEnum.REJECTED, "驳回失败");
    }

    public ApiDefect repair(Long projectId, ApiDefectHandle apiDefectHandle) {
        if (apiDefectHandle.getHandlerAccountList() == null || apiDefectHandle.getHandlerAccountList().isEmpty()) {
            throw new ApiException("处理人不能为空");
        }
        return updateDefectHandle(projectId, apiDefectHandle, ApiDefectStateEnum.AUDIT,
                ApiDefectLogStateEnum.REPAIR, "修复失败");
    }

    public ApiDefect pass(Long projectId, ApiDefectHandle apiDefectHandle) {
        return updateDefectHandle(projectId, apiDefectHandle, ApiDefectStateEnum.CLOSED,
                ApiDefectLogStateEnum.PASS, "通过失败");
    }

    public ApiDefect close(Long projectId, ApiDefectHandle apiDefectHandle) {
        if (apiDefectHandle.getDescribe() == null) {
            throw new ApiException("缺陷描述不能为空");
        }
        return updateDefectHandle(projectId, apiDefectHandle, ApiDefectStateEnum.CLOSED,
                ApiDefectLogStateEnum.CLOSED, "关闭失败");
    }

    public ApiDefect open(Long projectId, ApiDefectHandle apiDefectHandle) {
        if (apiDefectHandle.getDescribe() == null) {
            throw new ApiException("缺陷描述不能为空");
        }
        return updateDefectHandle(projectId, apiDefectHandle, ApiDefectStateEnum.PROCESSING,
                ApiDefectLogStateEnum.OPEN, "开启失败");
    }

    private ApiDefectRequest normalizeDefectLevelFilter(ApiDefectRequest apiDefect) {
        if (apiDefect == null) {
            apiDefect = new ApiDefectRequest();
        }
        if (StringUtils.isNotBlank(apiDefect.getDefectLevel())) {
            apiDefect.setDefectLevel(apiDefect.getDefectLevel().toLowerCase());
        }
        return apiDefect;
    }

    private Map<Long, ApiDeliverable> deliverablePathMap(Long projectId) {
        return openApiDeliverableService.selectApiDeliverablePathList(projectId).stream()
                .collect(Collectors.toMap(ApiDeliverable::getDeliverableId, d -> d));
    }

    private ApiDefect enrichDefect(ApiDefect defect, Map<Long, ApiDeliverable> pathMap) {
        if (defect == null) {
            return null;
        }
        if (defect.getDeliverableId() != null && pathMap.containsKey(defect.getDeliverableId())) {
            defect.setDeliverableName(pathMap.get(defect.getDeliverableId()).getDeliverablePath());
        }
        return normalizeDefectLevel(defect);
    }

    public ApiDefect normalizeDefectLevel(ApiDefect defect) {
        if (defect != null && StringUtils.isNotBlank(defect.getDefectLevel())) {
            defect.setDefectLevel(defect.getDefectLevel().toUpperCase());
        }
        return defect;
    }

    private ApiDefect updateDefectHandle(Long projectId, ApiDefectHandle apiDefectHandle,
                                         ApiDefectStateEnum apiDefectStateEnum,
                                         ApiDefectLogStateEnum apiDefectLogStateEnum,
                                         String exceptionMessage) {
        ApiDefect apiDefect = openApiDefectMapper.selectSysDefectByDefectNumber(projectId, apiDefectHandle.getDefectNum());
        if (apiDefect == null) {
            throw new ApiException("缺陷不存在");
        }

        ApiDefectRequest sd = new ApiDefectRequest();
        sd.setDefectId(apiDefect.getDefectId());
        if (apiDefectStateEnum != null) {
            sd.setDefectState(apiDefectStateEnum);
        }
        if (StringUtils.isNotBlank(apiDefectHandle.getDescribe())) {
            sd.setDefectDescribe(apiDefectHandle.getDescribe());
        }
        if (apiDefectHandle.getHandlerAccountList() != null && !apiDefectHandle.getHandlerAccountList().isEmpty()) {
            List<ApiMember> members = openApiMemberMapper.selectMemberByNames(projectId, apiDefectHandle.getHandlerAccountList());
            if (members.size() != apiDefectHandle.getHandlerAccountList().size()) {
                throw new ApiException("处理人不存在");
            }
            sd.setHandleBy(members.stream().map(ApiMember::getMemberId).collect(Collectors.toList()));
        }

        LoginUser loginUser = requireLoginUser();
        sd.setUpdateTime(new Date());
        sd.setUpdateBy(loginUser.getUsername());
        sd.setUpdateById(loginUser.getUserId());

        if (openApiDefectMapper.updateApiDefect(sd) <= 0) {
            throw new ApiException(exceptionMessage);
        }

        insertLog(apiDefect.getDefectId(), sd.getHandleBy(), apiDefectHandle.getDescribe(), apiDefectLogStateEnum);

        ApiDefect retDefect = selectSysDefectByDefectNumber(projectId, apiDefectHandle.getDefectNum());
        return retDefect;
    }

    private void resolveHandlers(Long projectId, ApiDefectRequest apiDefect) {
        if (apiDefect.getHandlerAccountList() == null || apiDefect.getHandlerAccountList().isEmpty()) {
            return;
        }
        List<ApiMember> members = openApiMemberMapper.selectMemberByNames(projectId, apiDefect.getHandlerAccountList());
        if (members.size() != apiDefect.getHandlerAccountList().size()) {
            throw new ApiException("处理人不存在");
        }
        apiDefect.setHandleBy(members.stream().map(ApiMember::getMemberId).collect(Collectors.toList()));
    }

    private LoginUser requireLoginUser() {
        LoginUser loginUser = authContext.getLoginUser();
        if (loginUser == null || loginUser.getUserId() == null) {
            throw new ApiException(401, "未授权");
        }
        if (loginUser.getUser() == null || StringUtils.isEmpty(loginUser.getUsername())) {
            SysUser sysUser = sysUserMapper.selectUserById(loginUser.getUserId());
            if (sysUser != null) {
                loginUser.setUser(sysUser);
            }
        }
        return loginUser;
    }

    private ApiDefectLog insertLog(Long defectId, List<Long> receives, String describe, ApiDefectLogStateEnum state) {
        ApiDefectLog sysDefectLog = new ApiDefectLog();
        sysDefectLog.setDefectId(defectId);
        sysDefectLog.setDefectLogDescribe(describe);
        sysDefectLog.setDefectLogState(state);
        LoginUser loginUser = requireLoginUser();
        sysDefectLog.setCreateBy(loginUser.getUserId().toString());
        sysDefectLog.setCreateById(loginUser.getUserId());
        sysDefectLog.setCreateTime(new Date());
        return openApiDefectLogMapper.insertApiDefectLog(sysDefectLog) > 0 ? sysDefectLog : null;
    }

    private List<Map<String, Object>> buildApiDefectChanges(ApiDefect oldDefect, ApiDefectRequest newDefect) {
        List<Map<String, Object>> changes = new ArrayList<>();

        addTextChange(changes, "defectName", oldDefect.getDefectName(), newDefect.getDefectName());
        addEnumChange(changes, "defectType",
                oldDefect.getDefectType() == null ? null : oldDefect.getDefectType().name(),
                newDefect.getDefectType() == null ? null : newDefect.getDefectType().name());
        addEnumChange(changes, "defectLevel", oldDefect.getDefectLevel(), newDefect.getDefectLevel());
        addTextChange(changes, "moduleVersion", oldDefect.getVersion(), newDefect.getVersion());

        if (!Objects.equals(nullSafe(oldDefect.getDeliverableName()), nullSafe(newDefect.getDeliverableName()))) {
            Map<String, Object> c = baseChange("moduleId", "module",
                    oldDefect.getDeliverableId(), newDefect.getDeliverableId());
            c.put("oldDisplay", oldDefect.getDeliverableName());
            c.put("newDisplay", newDefect.getDeliverableName());
            changes.add(c);
        }

        List<String> oldHandlers = oldDefect.getHandlerList() == null ? new ArrayList<>()
                : oldDefect.getHandlerList().stream()
                .map(ApiMemberBaseInfo::getMemberName).filter(Objects::nonNull)
                .collect(Collectors.toList());
        List<String> newHandlers = newDefect.getHandlerAccountList() == null ? new ArrayList<>()
                : newDefect.getHandlerAccountList();
        if (!oldHandlers.equals(newHandlers)) {
            Map<String, Object> c = baseChange("handleBy", "users", oldHandlers, newHandlers);
            c.put("oldDisplay", String.join(",", oldHandlers));
            c.put("newDisplay", String.join(",", newHandlers));
            changes.add(c);
        }

        addTextChange(changes, "defectDescribe", oldDefect.getDefectDescribe(), newDefect.getDefectDescribe());
        return changes;
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }

    private static void addTextChange(List<Map<String, Object>> changes, String field, String oldValue, String newValue) {
        if (!nullSafe(oldValue).equals(nullSafe(newValue))) {
            changes.add(baseChange(field, "text", oldValue, newValue));
        }
    }

    private static void addEnumChange(List<Map<String, Object>> changes, String field, String oldValue, String newValue) {
        if (!nullSafe(oldValue).equals(nullSafe(newValue))) {
            Map<String, Object> c = baseChange(field, "enum", oldValue, newValue);
            c.put("oldDisplay", oldValue);
            c.put("newDisplay", newValue);
            changes.add(c);
        }
    }

    private static Map<String, Object> baseChange(String field, String type, Object oldValue, Object newValue) {
        Map<String, Object> c = new HashMap<>();
        c.put("field", field);
        c.put("type", type);
        c.put("oldValue", oldValue);
        c.put("newValue", newValue);
        return c;
    }
}
