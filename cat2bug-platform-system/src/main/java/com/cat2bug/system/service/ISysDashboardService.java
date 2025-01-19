package com.cat2bug.system.service;

import com.cat2bug.system.domain.SysDefectLine;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 仪表盘服务
 */
public interface ISysDashboardService {
    /**
     * 缺陷折线图数据
     * @param projectId
     * @param timeType  时间类型 day, month
     * @return
     */
    public List<SysDefectLine> defectLine(Long projectId, String timeType);
}
