package com.cat2bug.system.service;

import com.cat2bug.system.domain.SysDefectOpenWorkload;
import com.cat2bug.system.domain.SysDefectOpenWorkloadSummary;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 缺陷统计服务
 */
public interface ISysDefectStatisticService {
    /**
     * 按照缺陷类型统计
     * @param projectId 项目id
     * @param memberId  成员id
     * @return
     */
    public List<Map<String,Object>> typeStatistic(Long projectId, Long memberId);
    /**
     * 按照缺陷状态统计
     * @param projectId 项目id
     * @param memberId  成员id
     * @return
     */
    public List<Map<String,Object>> stateStatistic(Long projectId, Long memberId);

    /**
     * 按照模块统计
     * @param projectId
     * @return
     */
    public List<Map<String, Object>> moduleStatistic(@Param("projectId") Long projectId);

    /**
     * 团队未关闭待办负载 Top5
     */
    List<SysDefectOpenWorkload> openWorkload(Long projectId);

    /**
     * 个人未关闭待办汇总
     */
    SysDefectOpenWorkloadSummary openWorkloadMy(Long projectId, Long userId);
}
