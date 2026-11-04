package com.cat2bug.system.util;

import com.cat2bug.common.domain.SysDefect;
import com.cat2bug.common.domain.SysDefectLog;
import com.cat2bug.system.mapper.SysDefectLogMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class DefectCycleMetricsEnricher {

    @Inject
    SysDefectLogMapper sysDefectLogMapper;

    public void enrich(List<SysDefect> defects) {
        if (defects == null || defects.isEmpty()) {
            return;
        }
        List<Long> defectIds = new ArrayList<>();
        for (SysDefect defect : defects) {
            if (defect != null && defect.getDefectId() != null) {
                defectIds.add(defect.getDefectId());
            }
        }
        Map<Long, List<SysDefectLog>> logsByDefect = loadLogs(defectIds);
        Date now = new Date();
        for (SysDefect defect : defects) {
            if (defect == null || defect.getDefectId() == null) {
                continue;
            }
            List<SysDefectLog> logs = logsByDefect.getOrDefault(defect.getDefectId(), Collections.emptyList());
            DefectCycleMetrics metrics = DefectCycleMetricsCalculator.compute(logs, defect.getCreateTime(), now);
            defect.setHandleStartTime(metrics.getHandleStartTime());
            defect.setHandlePassTime(metrics.getHandlePassTime());
            defect.setRejectCount(metrics.getRejectCount());
        }
    }

    public void enrich(SysDefect defect) {
        if (defect == null) {
            return;
        }
        enrich(List.of(defect));
    }

    private Map<Long, List<SysDefectLog>> loadLogs(List<Long> defectIds) {
        if (defectIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<SysDefectLog> rows = sysDefectLogMapper.selectCycleMetricLogsByDefectIds(defectIds);
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, List<SysDefectLog>> byDefect = new HashMap<>();
        for (SysDefectLog log : rows) {
            if (log == null || log.getDefectId() == null) {
                continue;
            }
            byDefect.computeIfAbsent(log.getDefectId(), id -> new ArrayList<>()).add(log);
        }
        return byDefect;
    }
}
