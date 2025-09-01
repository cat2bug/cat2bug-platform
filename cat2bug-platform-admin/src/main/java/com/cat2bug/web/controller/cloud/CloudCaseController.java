package com.cat2bug.web.controller.cloud;

import com.cat2bug.common.core.controller.BaseController;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.core.page.TableDataInfo;
import com.cat2bug.system.domain.SysCase;
import com.cat2bug.web.service.ICloudCaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-02-04 17:11
 * @Version: 1.0.0
 */
@RestController
@RequestMapping("/cloud/api/case")
public class CloudCaseController extends BaseController {
    @Autowired
    ICloudCaseService caseService;

    /**
     * 查询测试用例列表
     */
    @PreAuthorize("@ss.hasPermi('system:case:list')")
    @GetMapping("/list")
    public AjaxResult list(String content)
    {
        List<SysCase> list = caseService.searchCaseListOfAI(content);
        return success(list);
    }
}
