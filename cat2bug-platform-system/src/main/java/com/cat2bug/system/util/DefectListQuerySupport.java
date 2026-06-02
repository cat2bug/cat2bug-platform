package com.cat2bug.system.util;

import com.cat2bug.common.core.page.PageDomain;
import com.cat2bug.common.core.page.TableSupport;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.common.utils.sql.SqlUtil;
import com.github.pagehelper.PageHelper;

/**
 * 缺陷列表查询辅助：分页、排序参数校验等。
 */
public final class DefectListQuerySupport {

    private DefectListQuerySupport() {
    }

    /**
     * 表格自定义列 prop 为 custom_xxx，对应 JSON 字段而非物理列，不能用于 PageHelper ORDER BY。
     */
    public static boolean isCustomFieldOrderColumn(String orderByColumn) {
        return StringUtils.isNotEmpty(orderByColumn) && orderByColumn.startsWith("custom_");
    }

    /**
     * 启动缺陷列表分页；若排序列为 custom_ 前缀则忽略，避免 Unknown column 报错。
     */
    public static void startDefectListPage() {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        if (isCustomFieldOrderColumn(pageDomain.getOrderByColumn())) {
            pageDomain.setOrderByColumn(null);
            pageDomain.setIsAsc(null);
        }
        Integer pageNum = pageDomain.getPageNum();
        Integer pageSize = pageDomain.getPageSize();
        String orderBy = SqlUtil.escapeOrderBySql(pageDomain.getOrderBy());
        PageHelper.startPage(pageNum, pageSize, orderBy).setReasonable(pageDomain.getReasonable());
    }
}
