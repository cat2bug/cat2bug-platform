package com.cat2bug.api.service;

import com.cat2bug.api.domain.ApiDeliverable;

import java.util.List;
import java.util.Optional;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-11-03 01:06
 * @Version: 1.0.0
 * 交付物服务
 */
public interface IApiDeliverableService {
    public List<ApiDeliverable> selectApiDeliverableList(ApiDeliverable apiDeliverable);

    public List<ApiDeliverable> selectApiDeliverablePathList(Long projectId);

    /**
     * 当前项目全部交付物（扁平全路径 + 直接子节点数），用于一次展开树形列表。
     */
    List<ApiDeliverable> selectApiDeliverableTreeFlat(Long projectId);

    /**
     * 按全路径（多级 {@code /}）或节点名称解析为模块主键，未找到返回 empty
     */
    Optional<Long> resolveToModuleId(Long projectId, String deliverablePathOrName);

    /**
     * 父级全路径或名称解析为父模块主键；空串或 null 表示根（返回 0）
     */
    long resolveParentModulePid(Long projectId, String parentDeliverablePathOrName);

    /**
     * 按路径或名称得到用于 Open API 的交付物视图（不含数字 ID 字段的 JSON）
     */
    Optional<ApiDeliverable> buildApiViewByPathOrName(Long projectId, String deliverablePathOrName);
}
