package com.cat2bug.api.controller;

import com.cat2bug.api.domain.ApiDefect;
import com.cat2bug.api.service.IApiDefectService;
import com.cat2bug.common.core.controller.BaseController;
import com.cat2bug.common.core.page.TableDataInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-02-10 23:30
 * @Version: 1.0.0
 */
@RestController
@RequestMapping("/api/defect")
public class ApiDefectController extends BaseController {
    @Autowired
    private IApiDefectService apiDefectService;

    /**
     * 查询缺陷列表
     * @param apiDefect 缺陷参数对象
     * @return  缺陷集合
     */
    @PreAuthorize("@ss.hasPermi('api:defect:list')")
    @GetMapping()
    public TableDataInfo list(ApiDefect apiDefect)
    {
        startPage();
        List<ApiDefect> list = this.apiDefectService.selectApiDefectList(apiDefect);
        return getDataTable(list);
    }
}
