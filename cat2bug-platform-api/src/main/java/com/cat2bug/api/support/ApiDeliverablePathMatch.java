package com.cat2bug.api.support;

import com.cat2bug.api.domain.ApiDeliverable;

import java.util.List;
import java.util.Optional;

/**
 * 交付物全路径（多级用 {@code /}）或单层名称与 {@link ApiDeliverable} 列表的匹配。
 */
public final class ApiDeliverablePathMatch {
    private ApiDeliverablePathMatch() {
    }

    /**
     * 先按全路径 {@link ApiDeliverable#getDeliverablePath()} 精确匹配，再按 {@link ApiDeliverable#getDeliverableName()} 匹配。
     */
    public static Optional<ApiDeliverable> find(List<ApiDeliverable> pathList, String pathOrName) {
        if (pathList == null || pathOrName == null) {
            return Optional.empty();
        }
        String q = pathOrName.trim();
        if (q.isEmpty()) {
            return Optional.empty();
        }
        Optional<ApiDeliverable> byPath = pathList.stream().filter(d -> q.equals(d.getDeliverablePath())).findFirst();
        if (byPath.isPresent()) {
            return byPath;
        }
        return pathList.stream().filter(d -> q.equals(d.getDeliverableName())).findFirst();
    }
}
