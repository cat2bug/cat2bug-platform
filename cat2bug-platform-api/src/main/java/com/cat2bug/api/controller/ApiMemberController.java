package com.cat2bug.api.controller;

import com.cat2bug.api.domain.ApiCase;
import com.cat2bug.api.domain.ApiMember;
import com.cat2bug.api.service.IApiCaseService;
import com.cat2bug.api.service.impl.IApiMemberService;
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
@RequestMapping("/api/member")
public class ApiMemberController extends BaseController {
    @Resource
    private IApiMemberService apiMemberService;

    /**
     * 查询成员列表
     * @param apiMember 成员参数对象
     * @return  成员集合
     */
    @PreAuthorize("@ss.hasPermi('api:defect:list')")
    @GetMapping
    public TableDataInfo list(ApiMember apiMember)
    {
        List<ApiMember> list = this.apiMemberService.selectApiMemberList(apiMember);
        return getDataTable(list);
    }
}
