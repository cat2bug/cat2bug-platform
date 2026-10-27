package com.cat2bug.system.util;

import com.cat2bug.common.domain.dashboard.SysMemberWeeklyMetricRow;
import com.cat2bug.common.domain.dashboard.SysMemberWeeklyRankItem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 合并缺陷 / 文档 / 提交三维计数，过滤 total=0；按展示名升序（无排名语义），同名再按 userId。
 */
public final class SysMemberWeeklyRankHelper {

    private SysMemberWeeklyRankHelper() {
    }

    public static List<SysMemberWeeklyRankItem> mergeAndSort(
            List<SysMemberWeeklyMetricRow> defects,
            List<SysMemberWeeklyMetricRow> documents,
            Map<Long, Integer> commitCountsByUserId) {
        Map<Long, SysMemberWeeklyRankItem> byUser = new HashMap<>();
        applyMetric(byUser, defects, MetricKind.DEFECT);
        applyMetric(byUser, documents, MetricKind.FILE);
        if (commitCountsByUserId != null) {
            for (Map.Entry<Long, Integer> e : commitCountsByUserId.entrySet()) {
                if (e.getKey() == null || e.getValue() == null || e.getValue() <= 0) {
                    continue;
                }
                SysMemberWeeklyRankItem item = byUser.computeIfAbsent(e.getKey(), id -> {
                    SysMemberWeeklyRankItem row = new SysMemberWeeklyRankItem();
                    row.setUserId(id);
                    return row;
                });
                item.setCommitCount(item.getCommitCount() + e.getValue());
            }
        }
        List<SysMemberWeeklyRankItem> list = new ArrayList<>();
        for (SysMemberWeeklyRankItem item : byUser.values()) {
            int total = item.getDefectCount() + item.getCommitCount() + item.getFileCount();
            if (total <= 0) {
                continue;
            }
            item.setTotal(total);
            list.add(item);
        }
        list.sort(Comparator
                .comparing(SysMemberWeeklyRankHelper::displayName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(item -> item.getUserId() == null ? Long.MAX_VALUE : item.getUserId()));
        return list;
    }

    /** 供补齐头像/昵称后再次稳定排序。 */
    public static void sortByDisplayName(List<SysMemberWeeklyRankItem> members) {
        if (members == null || members.size() < 2) {
            return;
        }
        members.sort(Comparator
                .comparing(SysMemberWeeklyRankHelper::displayName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(item -> item.getUserId() == null ? Long.MAX_VALUE : item.getUserId()));
    }

    private static String displayName(SysMemberWeeklyRankItem item) {
        if (item == null) {
            return "";
        }
        if (item.getNickName() != null && !item.getNickName().isBlank()) {
            return item.getNickName();
        }
        if (item.getUserName() != null && !item.getUserName().isBlank()) {
            return item.getUserName();
        }
        return item.getUserId() == null ? "" : String.valueOf(item.getUserId());
    }

    private enum MetricKind {
        DEFECT, FILE
    }

    private static void applyMetric(Map<Long, SysMemberWeeklyRankItem> byUser,
                                    List<SysMemberWeeklyMetricRow> rows,
                                    MetricKind kind) {
        if (rows == null) {
            return;
        }
        for (SysMemberWeeklyMetricRow row : rows) {
            if (row == null || row.getUserId() == null || row.getMetricCount() <= 0) {
                continue;
            }
            SysMemberWeeklyRankItem item = byUser.computeIfAbsent(row.getUserId(), id -> {
                SysMemberWeeklyRankItem created = new SysMemberWeeklyRankItem();
                created.setUserId(id);
                return created;
            });
            if (item.getNickName() == null) {
                item.setNickName(row.getNickName());
            }
            if (item.getUserName() == null) {
                item.setUserName(row.getUserName());
            }
            if (item.getAvatar() == null) {
                item.setAvatar(row.getAvatar());
            }
            if (kind == MetricKind.DEFECT) {
                item.setDefectCount(row.getMetricCount());
            } else {
                item.setFileCount(row.getMetricCount());
            }
        }
    }
}
