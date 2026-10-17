package com.cat2bug.system.service;

import com.cat2bug.common.api.TableDataInfo;
import com.cat2bug.common.core.page.PageDomain;
import com.cat2bug.common.domain.LoginUser;
import com.cat2bug.common.domain.SysCase;
import com.cat2bug.common.domain.SysCaseStep;
import com.cat2bug.common.domain.SysUserConfig;
import com.cat2bug.common.domain.vo.ExcelImportResultVo;
import com.cat2bug.common.domain.vo.ExcelImportRowResultVo;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.common.exception.CaseImportValidationException;
import com.cat2bug.common.util.StringUtils;
import com.cat2bug.framework.security.AuthContext;
import com.cat2bug.system.mapper.SysCaseMapper;
import com.cat2bug.system.mapper.SysModuleMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@ApplicationScoped
public class SysCaseService {

    @Inject
    SysCaseMapper sysCaseMapper;

    @Inject
    SysModuleMapper sysModuleMapper;

    @Inject
    SysUserConfigService sysUserConfigService;

    @Inject
    AuthContext authContext;

    @Inject
    MemberFocusService memberFocusService;

    public TableDataInfo selectCasePage(SysCase query, Integer pageNum, Integer pageSize) {
        PageDomain page = PageDomain.of(pageNum, pageSize);
        long total = sysCaseMapper.countSysCaseList(query);
        var rows = sysCaseMapper.selectSysCaseList(query, page.getOffset(), page.getPageSize());
        return new TableDataInfo(rows, total);
    }

    /** 左侧交付物树点击：按该节点及其全部子交付物筛选用例。 */
    public void restrictToModuleTree(SysCase query, Long modulePid) {
        if (query == null || modulePid == null) {
            return;
        }
        Set<Long> moduleIds = sysModuleMapper.getAllChildIds(query.getProjectId(), modulePid);
        if (query.getParams() == null) {
            query.setParams(new HashMap<>());
        }
        if (moduleIds == null || moduleIds.isEmpty()) {
            query.getParams().put("moduleIds", List.of(-1L));
        } else {
            query.getParams().put("moduleIds", new ArrayList<>(moduleIds));
        }
        query.setModuleId(null);
    }

    public SysCase selectSysCaseByCaseId(Long caseId) {
        SysCase sysCase = sysCaseMapper.selectSysCaseByCaseId(caseId);
        if (sysCase == null) {
            throw new ApiException("测试用例不存在");
        }
        return sysCase;
    }

    public SysCase selectPrevSysCase(SysCase query, Long caseId) {
        if (query.getParams() == null) {
            query.setParams(new HashMap<>());
        }
        query.getParams().put("prevCaseId", caseId);
        return sysCaseMapper.selectPrevSysCase(query);
    }

    public SysCase selectNextSysCase(SysCase query, Long caseId) {
        if (query.getParams() == null) {
            query.setParams(new HashMap<>());
        }
        query.getParams().put("nextCaseId", caseId);
        return sysCaseMapper.selectNextSysCase(query);
    }

    public SysCase selectSysCaseByCaseNum(Long projectId, Long caseNum) {
        return sysCaseMapper.selectSysCaseByCaseNum(projectId, caseNum);
    }

    public int deleteSysCaseByCaseNum(Long projectId, Long caseNum) {
        return sysCaseMapper.deleteSysCaseByCaseNumAndProjectId(projectId, caseNum);
    }

    public SysCase insertSysCase(SysCase sysCase) {
        LoginUser loginUser = requireLoginUser();
        Date now = new Date();
        long maxNum = sysCaseMapper.getCaseMaxNumOfProject(sysCase.getProjectId());
        sysCase.setCaseNum(maxNum + 1);
        sysCase.setCreateBy(loginUser.getUsername());
        sysCase.setUpdateBy(loginUser.getUsername());
        sysCase.setCreateTime(now);
        sysCase.setUpdateTime(now);
        if (sysCaseMapper.insertSysCase(sysCase) <= 0) {
            throw new ApiException("新增测试用例失败");
        }
        return sysCase;
    }

    @Transactional
    public List<SysCase> batchInsertSysCase(List<SysCase> list) {
        if (list == null || list.isEmpty()) {
            throw new ApiException("批量新增用例列表不能为空");
        }
        LoginUser loginUser = requireLoginUser();
        Date now = new Date();
        for (SysCase sysCase : list) {
            long maxNum = sysCaseMapper.getCaseMaxNumOfProject(sysCase.getProjectId());
            sysCase.setCaseNum(maxNum + 1);
            sysCase.setCreateBy(String.valueOf(loginUser.getUserId()));
            sysCase.setUpdateBy(String.valueOf(loginUser.getUserId()));
            sysCase.setCreateTime(now);
            sysCase.setUpdateTime(now);
            if (sysCaseMapper.insertSysCase(sysCase) <= 0) {
                throw new ApiException("批量新增测试用例失败");
            }
        }
        return list;
    }

    public int updateSysCase(SysCase sysCase) {
        if (sysCase.getCaseId() == null) {
            throw new ApiException("用例ID不能为空");
        }
        selectSysCaseByCaseId(sysCase.getCaseId());
        LoginUser loginUser = requireLoginUser();
        sysCase.setUpdateBy(loginUser.getUsername());
        sysCase.setUpdateTime(new Date());
        return sysCaseMapper.updateSysCase(sysCase);
    }

    public int deleteSysCaseByCaseId(Long caseId) {
        selectSysCaseByCaseId(caseId);
        return sysCaseMapper.deleteSysCaseByCaseId(caseId);
    }

    public List<Long> selectSysCaseIdList(Long moduleId) {
        Long projectId = requireCurrentProjectId();
        Set<Long> moduleIds = sysModuleMapper.getAllChildIds(projectId, moduleId);
        if (moduleIds == null || moduleIds.isEmpty()) {
            return Collections.emptyList();
        }
        return sysCaseMapper.selectSysCaseIdList(moduleIds);
    }

    public List<Long> selectSysCaseIdByLevelList(Long level) {
        Long projectId = requireCurrentProjectId();
        return sysCaseMapper.selectSysCaseIdListByLevel(projectId, level);
    }

    public List<SysCase> selectSysCaseListAll(SysCase query) {
        return sysCaseMapper.selectSysCaseListAll(query);
    }

    @Transactional
    public ExcelImportResultVo importCase(List<SysCase> caseList, Long projectId) {
        final String numPropertyName = "num";
        if (caseList == null || caseList.isEmpty()) {
            throw new ApiException("导入用例数据为空或格式不合法，请检查后重新导入！");
        }
        List<ExcelImportRowResultVo> rows = new ArrayList<>();
        int rowNum = 2;
        Collections.reverse(caseList);
        for (SysCase c : caseList) {
            ExcelImportRowResultVo rr = new ExcelImportRowResultVo();
            rr.setRowNum(rowNum);
            rr.setMessages(new ArrayList<>());
            if (c == null) {
                rr.getMessages().add("行数据为空，可能是列标题与系统默认标题不符!");
                rows.add(rr);
                continue;
            }
            Map<String, Object> params = new HashMap<>();
            params.put(numPropertyName, rowNum);
            c.setParams(params);

            if (StringUtils.isEmpty(c.getCaseName())) {
                rr.getMessages().add("用例名称不能为空!");
            } else if (c.getCaseName().length() > 255) {
                rr.getMessages().add("用例名称不能大于255个字符!");
            }
            if (StringUtils.isEmpty(c.getModuleName())) {
                rr.getMessages().add("交付物不能为空!");
            }
            if (StringUtils.isEmpty(c.getCaseExpect())) {
                rr.getMessages().add("预期不能为空!");
            } else if (c.getCaseExpect().length() > 65536) {
                rr.getMessages().add("预期不能大于1024个字符!");
            }
            if (StringUtils.isNotBlank(c.getCasePreconditions()) && c.getCasePreconditions().length() > 65536) {
                rr.getMessages().add("前置条件不能大于5000个字符!");
            }
            if (StringUtils.isNotBlank(c.getCaseData()) && c.getCaseData().length() > 65536) {
                rr.getMessages().add("用例数据不能大于10000个字符!");
            }

            rowNum++;
            if (!rr.getMessages().isEmpty()) {
                rows.add(rr);
            }
        }

        List<SysCase> validCases = caseList.stream()
                .filter(c -> StringUtils.isNotEmpty(c.getCaseName())
                        && StringUtils.isNotEmpty(c.getCaseExpect())
                        && StringUtils.isNotEmpty(c.getModuleName()))
                .collect(Collectors.toList());

        validCases.stream()
                .collect(Collectors.groupingBy((Function<SysCase, String>) sysCase ->
                        sysCase.getProjectId() + sysCase.getCaseName() + sysCase.getModuleId()))
                .values().stream()
                .filter(l -> l.size() > 1)
                .sorted(Comparator.comparingInt(l ->
                        Integer.parseInt(String.valueOf(l.get(0).getParams().get(numPropertyName)))))
                .forEach(l -> {
                    ExcelImportRowResultVo rr = new ExcelImportRowResultVo();
                    rr.setMessages(new ArrayList<>());
                    rr.getMessages().add(String.format("第 %s 行数据重复, 测试用例的用例名称与交付物组合不能重复!",
                            l.stream().map(c -> String.valueOf(c.getParams().get(numPropertyName)))
                                    .collect(Collectors.joining(","))));
                    rows.add(rr);
                });

        if (rows.isEmpty()) {
            LoginUser loginUser = requireLoginUser();
            long num = sysCaseMapper.getCaseMaxNumOfProject(projectId);
            rowNum = 2;
            List<Integer> repeatRowList = new ArrayList<>();
            Date now = new Date();
            for (SysCase c : caseList) {
                c.setCaseNum(++num);
                c.setProjectId(projectId);
                c.setCreateBy(loginUser.getUsername());
                c.setUpdateBy(loginUser.getUsername());
                c.setCreateTime(now);
                c.setUpdateTime(now);
                if (StringUtils.isNotBlank(c.getImgObjects())) {
                    c.setImgUrls(c.getImgObjects());
                }
                if (c.getModuleId() == null && StringUtils.isNotBlank(c.getModuleName())) {
                    try {
                        c.setModuleId(Long.parseLong(c.getModuleName()));
                    } catch (NumberFormatException ignored) {
                        // module path resolved in import parser
                    }
                }
                if (StringUtils.isNotBlank(c.getCaseStepScript())) {
                    String[] rowSteps = c.getCaseStepScript().split("\n");
                    List<SysCaseStep> steps = Arrays.stream(rowSteps).map(s -> {
                        SysCaseStep cs = new SysCaseStep();
                        String[] parts = s.split("---");
                        cs.setStepDescribe(parts[0]);
                        if (parts.length > 1) {
                            cs.setStepExpect(parts[1]);
                        }
                        return cs;
                    }).collect(Collectors.toList());
                    c.setCaseStep(steps);
                }
                int count = sysCaseMapper.insertSysCase(c);
                if (count == 0) {
                    repeatRowList.add(rowNum++);
                }
            }
            if (!repeatRowList.isEmpty()) {
                ExcelImportRowResultVo rr = new ExcelImportRowResultVo();
                rr.setMessages(List.of(String.format("第 %s 行数据已在数据库中存在, 同一项目下的测试用例名称与交付物的组合不能重复!",
                        repeatRowList.stream().map(String::valueOf).collect(Collectors.joining("、")))));
                rr.setRowCount(repeatRowList.size());
                rows.add(rr);
            }
        }

        ExcelImportResultVo ret = new ExcelImportResultVo();
        ret.setRows(rows);
        if (!rows.isEmpty()) {
            ret.setMessage(String.format("很抱歉，导入失败！共 %d 条数据格式不正确，错误如下：",
                    rows.stream().map(ExcelImportRowResultVo::getRowCount).reduce(0, Integer::sum)));
            throw new CaseImportValidationException(ret);
        }
        ret.setMessage("恭喜您，数据已全部导入成功！");
        return ret;
    }

    /** 关闭前端编辑窗口（清除成员编辑焦点）。 */
    public void closeEditWindow() {
        LoginUser loginUser = authContext.getLoginUser();
        if (loginUser != null) {
            memberFocusService.removeFocus(loginUser.getUserId());
        }
    }

    private Long requireCurrentProjectId() {
        LoginUser loginUser = requireLoginUser();
        SysUserConfig config = sysUserConfigService.selectSysUserConfigByUserId(loginUser.getUserId());
        if (config == null || config.getCurrentProjectId() == null) {
            throw new ApiException("当前项目未设置");
        }
        return config.getCurrentProjectId();
    }

    private LoginUser requireLoginUser() {
        LoginUser loginUser = authContext.getLoginUser();
        if (loginUser == null) {
            throw new ApiException(401, "未登录或会话已失效");
        }
        return loginUser;
    }
}
