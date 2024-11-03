package com.cat2bug.api.service;

import com.cat2bug.api.domain.ApiDeliverable;

import java.util.List;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-11-03 01:06
 * @Version: 1.0.0
 * 交付物服务
 */
public interface IApiDeliverableService {
    public List<ApiDeliverable> selectApiDeliverableList(ApiDeliverable apiDeliverable);

    public List<ApiDeliverable> selectApiDeliverablePathList(Long projectId);
}
