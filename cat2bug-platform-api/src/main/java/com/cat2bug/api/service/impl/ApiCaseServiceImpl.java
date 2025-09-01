package com.cat2bug.api.service.impl;

import com.cat2bug.api.domain.ApiCase;
import com.cat2bug.api.domain.ApiDeliverable;
import com.cat2bug.api.mapper.ApiCaseMapper;
import com.cat2bug.api.mapper.ApiDeliverableMapper;
import com.cat2bug.api.service.ApiService;
import com.cat2bug.api.service.IApiCaseService;
import com.cat2bug.common.utils.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-11-03 10:07
 * @Version: 1.0.0
 */
@Service
public class ApiCaseServiceImpl implements IApiCaseService {
    @Resource
    private ApiCaseMapper apiCaseMapper;
    @Resource
    private ApiService apiService;
    @Resource
    private ApiDeliverableMapper apiDeliverableMapper;

    @Override
    public List<ApiCase> selectApiCaseList(ApiCase apiCase) {
        final Long projectId = this.apiService.getProjectId();
        // 获取交付物列表，用于转换数据
        List<ApiDeliverable> deliverableList = this.apiDeliverableMapper.selectApiDeliverablePathList(projectId);

        // 根据交付物的全路径取查询要查询的交付物ID
        if(StringUtils.isNotBlank(apiCase.getDeliverableName())) {
            Optional<ApiDeliverable> deliverable = deliverableList.stream().filter(c->c.getDeliverableName().equals(apiCase.getDeliverableName())).findFirst();
            // 如果要查询的交付物路径在数据库中存在，就设置交付物ID，否则设置-1使之无法查到
            if(deliverable.isPresent()) {
                apiCase.setDeliverableId(deliverable.get().getDeliverableId());
            } else {
                apiCase.setDeliverableId(-1L);
            }
        }

        // 制作一个交付物全路径Map，用于查找转换
        final Map<Long, ApiDeliverable> allPathApiDeliverableMap = deliverableList.
                stream().collect(Collectors.toMap(ApiDeliverable::getDeliverableId, d->d));

        return this.apiCaseMapper.selectApiCaseList(projectId, apiCase).stream().map(c->{
            // 如果交付物ID不为空，且在全路径字典里存在，就这是交付物名称为全路径名称，否则默认为空。
            if(c.getDeliverableId()!=null &&
                    allPathApiDeliverableMap.containsKey(c.getDeliverableId())) {
                // 设置交付物全路径名
                c.setDeliverableName(allPathApiDeliverableMap.get(c.getDeliverableId()).getDeliverablePath());
            }
            return c;
        }).collect(Collectors.toList());
    }
}
