package com.cat2bug.system.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 缺陷统计接口
 */
public interface SysDefectStatisticMapper {
    /**
     * 按照缺陷类型统计
     * @param projectId 项目id
     * @param memberId  成员id
     * @return
     */
    public List<Map<String,Object>> typeStatistic(@Param("projectId") Long projectId, @Param("memberId") Long memberId);
}
