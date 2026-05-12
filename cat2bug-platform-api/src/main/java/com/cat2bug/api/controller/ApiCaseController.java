package com.cat2bug.api.controller;

import com.cat2bug.api.domain.ApiCase;
import com.cat2bug.api.domain.ApiCaseStep;
import com.cat2bug.api.domain.ApiCaseWriteRequest;
import com.cat2bug.api.service.ApiService;
import com.cat2bug.api.service.IApiCaseService;
import com.cat2bug.api.service.IApiDeliverableService;
import com.cat2bug.common.core.controller.BaseController;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.core.page.TableDataInfo;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.system.domain.SysCase;
import com.cat2bug.system.domain.SysCaseStep;
import com.cat2bug.system.service.ISysCaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-11-03 01:19
 * @Version: 1.0.0
 */
@RestController
@RequestMapping("/api/case")
public class ApiCaseController extends BaseController {
    @Resource
    private IApiCaseService apiCaseService;

    @Resource
    private ApiService apiService;

    @Autowired
    private ISysCaseService sysCaseService;

    @Resource
    private IApiDeliverableService apiDeliverableService;

    /**
     * 查询测试用例列表
     * @param apiCase 测试用例参数对象
     * @return  测试用例集合
     */
    @GetMapping
    public TableDataInfo list(ApiCase apiCase)
    {
        startPage();
        List<ApiCase> list = this.apiCaseService.selectApiCaseList(apiCase);
        return getDataTable(list);
    }

    /**
     * 查询测试用例详情（路径参数为用例编号 caseNum）
     */
    @GetMapping("/{caseNum}")
    public AjaxResult getInfo(@PathVariable("caseNum") Long caseNum)
    {
        ApiCase apiCase = this.apiCaseService.selectApiCaseByCaseNum(caseNum);
        if (apiCase == null) {
            return AjaxResult.error("用例不存在或无权限访问");
        }
        return AjaxResult.success(apiCase);
    }

    /**
     * 新增测试用例
     */
    @PostMapping
    public AjaxResult add(@RequestBody ApiCaseWriteRequest body)
    {
        if (StringUtils.isBlank(body.getCaseName()) || StringUtils.isBlank(body.getCaseExpect()) || body.getCaseLevel() == null) {
            return AjaxResult.error("caseName、caseExpect、caseLevel 为必填");
        }
        Long projectId = this.apiService.getProjectId();
        SysCase sysCase = new SysCase();
        sysCase.setCaseName(body.getCaseName());
        sysCase.setCaseExpect(body.getCaseExpect());
        sysCase.setCaseStep(toSysCaseSteps(body.getCaseStep()));
        sysCase.setCaseLevel(body.getCaseLevel());
        sysCase.setCasePreconditions(body.getCasePreconditions());
        sysCase.setCaseData(body.getCaseData());
        sysCase.setRemark(body.getRemark());
        if (StringUtils.isNotBlank(body.getDeliverableName())) {
            Long moduleId = this.apiDeliverableService.resolveToModuleId(projectId, body.getDeliverableName()).orElse(null);
            if (moduleId == null) {
                return AjaxResult.error("交付物不存在: " + body.getDeliverableName());
            }
            sysCase.setModuleId(moduleId);
        }
        sysCase.setProjectId(projectId);
        SysCase result = sysCaseService.insertSysCase(sysCase);
        ApiCase view = this.apiCaseService.selectApiCaseByCaseNum(result.getCaseNum());
        return AjaxResult.success(view);
    }

    /**
     * 修改测试用例（按请求体 {@code caseNum} 定位，不使用 caseId）
     */
    @PutMapping
    public AjaxResult edit(@RequestBody ApiCaseWriteRequest body)
    {
        if (body.getCaseNum() == null) {
            return AjaxResult.error("caseNum 为必填（用例编号）");
        }
        Long projectId = this.apiService.getProjectId();
        SysCase existing = this.sysCaseService.selectSysCaseByCaseNum(projectId, body.getCaseNum());
        if (existing == null) {
            return AjaxResult.error("用例不存在或无权限访问");
        }
        if (body.getCaseName() != null) {
            existing.setCaseName(body.getCaseName());
        }
        if (body.getCaseExpect() != null) {
            existing.setCaseExpect(body.getCaseExpect());
        }
        if (body.getCaseStep() != null) {
            existing.setCaseStep(toSysCaseSteps(body.getCaseStep()));
        }
        if (body.getCaseLevel() != null) {
            existing.setCaseLevel(body.getCaseLevel());
        }
        if (body.getCasePreconditions() != null) {
            existing.setCasePreconditions(body.getCasePreconditions());
        }
        if (body.getCaseData() != null) {
            existing.setCaseData(body.getCaseData());
        }
        if (body.getRemark() != null) {
            existing.setRemark(body.getRemark());
        }
        if (body.getDeliverableName() != null) {
            if (StringUtils.isBlank(body.getDeliverableName())) {
                existing.setModuleId(null);
            } else {
                Long moduleId = this.apiDeliverableService.resolveToModuleId(projectId, body.getDeliverableName()).orElse(null);
                if (moduleId == null) {
                    return AjaxResult.error("交付物不存在: " + body.getDeliverableName());
                }
                existing.setModuleId(moduleId);
            }
        }
        this.sysCaseService.updateSysCase(existing);
        return AjaxResult.success(this.apiCaseService.selectApiCaseByCaseNum(existing.getCaseNum()));
    }

    /**
     * 删除测试用例（路径参数为用例编号 caseNum）
     */
    @DeleteMapping("/{caseNum}")
    public AjaxResult remove(@PathVariable("caseNum") Long caseNum)
    {
        Long projectId = this.apiService.getProjectId();
        int rows = this.sysCaseService.deleteSysCaseByCaseNum(projectId, caseNum);
        if (rows == 0) {
            return AjaxResult.error("用例不存在或无权限访问");
        }
        return AjaxResult.success();
    }

    private static List<SysCaseStep> toSysCaseSteps(List<ApiCaseStep> steps) {
        if (steps == null) {
            return null;
        }
        return steps.stream().map(s -> {
            SysCaseStep t = new SysCaseStep();
            t.setStepDescribe(s.getStepDescribe());
            t.setStepExpect(s.getStepExpect());
            return t;
        }).collect(Collectors.toList());
    }
}
