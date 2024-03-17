package com.cat2bug.api.controller;

import com.cat2bug.api.domain.ApiDefect;
import com.cat2bug.api.service.IApiDefectService;
import com.cat2bug.common.core.controller.BaseController;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.core.page.TableDataInfo;
import com.cat2bug.common.utils.StringUtils;
import io.swagger.annotations.Api;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-02-10 23:30
 * @Version: 1.0.0
 */
//@RestController
//@RequestMapping("/api/defect")
//@Api(tags = "缺陷")
public class ApiDefectController extends BaseController {
    @Autowired
    private IApiDefectService apiDefectService;

    /**
     * 查询缺陷列表
     * @param apiDefect 缺陷参数对象
     * @return  缺陷集合
     */
    @PreAuthorize("@ss.hasPermi('api:defect:list')")
    @GetMapping
    public TableDataInfo list(ApiDefect apiDefect)
    {
        startPage();
        List<ApiDefect> list = this.apiDefectService.selectApiDefectList(apiDefect);
        return getDataTable(list);
    }

    /**
     * 获取缺陷详细信息
     */
    @PreAuthorize("@ss.hasPermi('api:defect:query')")
    @GetMapping(value = "/{defectNumber}")
    public AjaxResult getInfo(@PathVariable("defectNumber") Long defectNumber)
    {
        return success(apiDefectService.selectSysDefectByDefectNumber(defectNumber));
    }

    /**
     * 新增缺陷
     */
    @Operation(summary = "添加缺陷")
    @Parameters({
            @Parameter(name = "defectName", description = "缺陷名称", required = true, in = ParameterIn.QUERY),
            @Parameter(name = "defectDescribe", description = "缺陷描述", required = true, in = ParameterIn.QUERY)
    })
    @PreAuthorize("@ss.hasPermi('api:defect:add')")
    @PostMapping
    public AjaxResult add(@RequestBody ApiDefect apiDefect)
    {
        return success(apiDefectService.insertApiDefect(apiDefect));
    }
}
