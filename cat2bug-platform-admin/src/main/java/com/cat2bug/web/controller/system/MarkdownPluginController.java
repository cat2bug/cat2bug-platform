package com.cat2bug.web.controller.system;

import com.cat2bug.common.core.controller.BaseController;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.core.domain.entity.SysDefect;
import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.system.domain.SysCase;
import com.cat2bug.system.domain.SysPlan;
import com.cat2bug.system.service.ISysCaseService;
import com.cat2bug.system.service.ISysDefectService;
import com.cat2bug.system.service.ISysPlanService;
import com.cat2bug.system.service.ISysUserProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-05-06 10:06
 * @Version: 1.0.0
 */
@RestController
@RequestMapping("/markdown")
public class MarkdownPluginController extends BaseController {
    @Autowired
    private ISysUserProjectService sysUserProjectService;
    @Autowired
    private ISysCaseService sysCaseService;
    @Autowired
    private ISysDefectService sysDefectService;
    @Autowired
    private ISysPlanService sysPlanService;

    @PreAuthorize("@ss.hasPermi('system:report:list')")
    @GetMapping("/{projectId}")
    public AjaxResult initData(@PathVariable("projectId") Long projectId)
    {
        Map<String, Object> ret = new HashMap<>();

        SysDefect sysDefect = new SysDefect();
        sysDefect.setProjectId(projectId);
        List<SysDefect> defectList = sysDefectService.selectSysDefectList(sysDefect);
        Map<String,Object> defectParam = new HashMap<>();
        defectParam.put("list",defectList);
        defectParam.put("total",defectList.size());
        ret.put("defect",defectParam);

        // 设置成员
        Map<String, Object> member = new HashMap<>();
        ret.put("member",member);
        List<SysUser> list = sysUserProjectService.selectSysUserListByProjectId(projectId, new SysUser());
        list = list.stream().map(u->{
            // 设置角色
            if(u.getRoles()!=null){
                u.setRoleIds(u.getRoles().stream().map(r->r.getRoleId()).collect(Collectors.toList()).toArray(new Long[]{}));
            }
            return u;
        }).collect(Collectors.toList());
        member.put("all",list);
        // 设置用例
        SysCase sysCase = new SysCase();
        sysCase.setProjectId(projectId);

        Map<String,Object> caseParam = new HashMap<>();
        ret.put("case",caseParam);
        List<SysCase> caseList = sysCaseService.selectSysCaseList(sysCase);
        caseParam.put("list",caseList);
        caseParam.put("total",caseList.size());

        Map<String,Object> planParam = new HashMap<>();
        ret.put("plan",planParam);
        SysPlan sysPlan = new SysPlan();
        sysPlan.setProjectId(projectId);
        List<SysPlan> planList = sysPlanService.selectSysPlanList(sysPlan);
        planParam.put("list", planList);
        return success(ret);
    }

}
