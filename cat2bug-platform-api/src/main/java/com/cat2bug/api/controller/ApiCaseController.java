package com.cat2bug.api.controller;

import com.cat2bug.api.domain.ApiCase;
import com.cat2bug.api.service.IApiCaseService;
import com.cat2bug.common.core.controller.BaseController;
import com.cat2bug.common.core.page.TableDataInfo;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    /**
     * 查询缺陷列表
     * @param apiCase 测试用例参数对象
     * @return  交付物集合
     */
    @PreAuthorize("@ss.hasPermi('api:defect:list')")
    @GetMapping
    public TableDataInfo list(ApiCase apiCase)
    {
        startPage();
        List<ApiCase> list = this.apiCaseService.selectApiCaseList(apiCase);
        return getDataTable(list);
    }
}
