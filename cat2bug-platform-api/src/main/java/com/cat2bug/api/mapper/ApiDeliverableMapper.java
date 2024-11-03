package com.cat2bug.api.mapper;

import com.cat2bug.api.domain.ApiDeliverable;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-11-03 01:08
 * @Version: 1.0.0
 */
@Mapper
public interface ApiDeliverableMapper {
    public List<ApiDeliverable> selectApiDeliverableList(@Param("projectId") Long projectId, @Param("apiDeliverable")ApiDeliverable apiDeliverable);

    /**
     * 查询模块路径列表
     * @param projectId 项目id
     * @return  模块路径集合
     */
    public List<ApiDeliverable> selectApiDeliverablePathList(Long projectId);
}
