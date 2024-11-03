package com.cat2bug.api.service.impl;

import com.cat2bug.api.domain.ApiDeliverable;
import com.cat2bug.api.mapper.ApiDeliverableMapper;
import com.cat2bug.api.service.ApiService;
import com.cat2bug.api.service.IApiDeliverableService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-11-03 01:09
 * @Version: 1.0.0
 */
@Service
public class ApiDeliverableServiceImpl implements IApiDeliverableService {

    @Resource
    private ApiService apiService;

    @Resource
    private ApiDeliverableMapper apiDeliverableMapper;

    @Override
    public List<ApiDeliverable> selectApiDeliverableList(ApiDeliverable apiDeliverable) {
        Long projectId = this.apiService.getProjectId();
        Map<Long, ApiDeliverable> allPathApiDeliverableMap = this.apiDeliverableMapper.selectApiDeliverablePathList(projectId).
                stream().collect(Collectors.toMap(ApiDeliverable::getDeliverableId, d->d));
        return apiDeliverableMapper.selectApiDeliverableList(projectId, apiDeliverable).stream().map(d->{
            // 设置交付物全路径
            if(allPathApiDeliverableMap.containsKey(d.getDeliverableId())) {
                d.setDeliverablePath(allPathApiDeliverableMap.get(d.getDeliverableId()).getDeliverablePath());
            }
            return d;
        }).collect(Collectors.toList());
    }

    @Override
    public List<ApiDeliverable> selectApiDeliverablePathList(Long projectId) {
        return this.apiDeliverableMapper.selectApiDeliverablePathList(projectId);
    }
}
