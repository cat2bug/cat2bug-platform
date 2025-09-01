package com.cat2bug.api.controller;

import com.cat2bug.api.domain.ApiDefect;
import com.cat2bug.api.domain.ApiDeliverable;
import com.cat2bug.api.service.IApiDefectService;
import com.cat2bug.api.service.IApiDeliverableService;
import com.cat2bug.common.core.controller.BaseController;
import com.cat2bug.common.core.page.TableDataInfo;
import org.springframework.beans.factory.annotation.Autowired;
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
@RequestMapping("/api/deliverable")
public class ApiDeliverableController extends BaseController {
    @Resource
    private IApiDeliverableService apiDeliverableService;

    /**
     * 查询缺陷列表
     * @param apiDeliverable 交付物参数对象
     * @return  交付物集合
     */
    @PreAuthorize("@ss.hasPermi('api:defect:list')")
    @GetMapping
    public TableDataInfo list(ApiDeliverable apiDeliverable)
    {
        if(apiDeliverable.getDeliverablePid()==null) {
            apiDeliverable.setDeliverablePid(0L);
        }
        List<ApiDeliverable> list = this.apiDeliverableService.selectApiDeliverableList(apiDeliverable);
        return getDataTable(list);
    }
}
