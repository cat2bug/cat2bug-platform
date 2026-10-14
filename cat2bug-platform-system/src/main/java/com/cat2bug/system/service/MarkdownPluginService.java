package com.cat2bug.system.service;

import com.cat2bug.common.domain.SysCase;
import com.cat2bug.common.domain.SysDefect;
import com.cat2bug.common.domain.SysPlan;
import com.cat2bug.common.domain.SysUser;
import com.cat2bug.system.mapper.SysDefectMapper;
import com.cat2bug.system.mapper.SysPlanMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class MarkdownPluginService {

    @Inject
    SysDefectMapper sysDefectMapper;

    @Inject
    SysCaseService sysCaseService;

    @Inject
    SysPlanMapper sysPlanMapper;

    @Inject
    SysUserProjectService sysUserProjectService;

    public Map<String, Object> initProjectData(Long projectId) {
        Map<String, Object> ret = new HashMap<>();

        SysDefect defectQuery = new SysDefect();
        defectQuery.setProjectId(projectId);
        List<SysDefect> defectList = sysDefectMapper.selectSysDefectList(defectQuery, 0, Integer.MAX_VALUE);
        Map<String, Object> defectParam = new HashMap<>();
        defectParam.put("list", defectList);
        defectParam.put("total", defectList.size());
        ret.put("defect", defectParam);

        Map<String, Object> member = new HashMap<>();
        ret.put("member", member);
        List<SysUser> users = MemberViewHelper.withRoleIds(
                sysUserProjectService.selectSysUserListByProjectId(projectId)).stream()
                .map(u -> {
                    if (u.getRoles() != null) {
                        u.setRoleIds(u.getRoles().stream()
                                .map(r -> r.getRoleId())
                                .toArray(Long[]::new));
                    }
                    return u;
                })
                .collect(Collectors.toList());
        member.put("all", users);

        SysCase caseQuery = new SysCase();
        caseQuery.setProjectId(projectId);
        List<SysCase> caseList = sysCaseService.selectSysCaseListAll(caseQuery);
        Map<String, Object> caseParam = new HashMap<>();
        caseParam.put("list", caseList);
        caseParam.put("total", caseList.size());
        ret.put("case", caseParam);

        SysPlan planQuery = new SysPlan();
        planQuery.setProjectId(projectId);
        List<SysPlan> planList = sysPlanMapper.selectSysPlanList(planQuery, 0, Integer.MAX_VALUE);
        Map<String, Object> planParam = new HashMap<>();
        planParam.put("list", planList);
        ret.put("plan", planParam);
        return ret;
    }
}
