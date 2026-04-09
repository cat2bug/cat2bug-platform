package com.cat2bug.api.controller;

import com.cat2bug.api.domain.ApiCase;
import com.cat2bug.api.service.IApiCaseService;
import com.cat2bug.common.core.controller.BaseController;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.core.page.TableDataInfo;
import com.cat2bug.system.domain.SysCase;
import com.cat2bug.system.service.ISysCaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

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

    @Autowired
    private ISysCaseService sysCaseService;

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
     * 查询测试用例详情
     * @param caseId 测试用例ID
     * @return 测试用例详情
     */
    @GetMapping("/{caseId}")
    public AjaxResult getInfo(@PathVariable("caseId") Long caseId)
    {
        SysCase sysCase = sysCaseService.selectSysCaseByCaseId(caseId);
        return AjaxResult.success(sysCase);
    }

    /**
     * 新增测试用例
     * @param sysCase 测试用例对象
     * @return 结果
     */
    @PostMapping
    public AjaxResult add(@RequestBody SysCase sysCase)
    {
        SysCase result = sysCaseService.insertSysCase(sysCase);
        return AjaxResult.success(result);
    }

    /**
     * 修改测试用例
     * @param sysCase 测试用例对象
     * @return 结果
     */
    @PutMapping
    public AjaxResult edit(@RequestBody SysCase sysCase)
    {
        return toAjax(sysCaseService.updateSysCase(sysCase));
    }

    /**
     * 删除测试用例
     * @param caseId 测试用例ID
     * @return 结果
     */
    @DeleteMapping("/{caseId}")
    public AjaxResult remove(@PathVariable("caseId") Long caseId)
    {
        return toAjax(sysCaseService.deleteSysCaseByCaseId(caseId));
    }
}
