package com.cat2bug.system.service;

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
}
