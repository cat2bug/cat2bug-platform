package com.cat2bug.common.core.page;

/**
 * 与 Spring PageDomain 默认行为对齐的分页参数。
 */
public class PageDomain {

    private static final int DEFAULT_PAGE_NUM = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;

    private final int pageNum;
    private final int pageSize;

    public PageDomain(Integer pageNum, Integer pageSize) {
        this.pageNum = normalizePageNum(pageNum);
        this.pageSize = normalizePageSize(pageSize);
    }

    public static PageDomain of(Integer pageNum, Integer pageSize) {
        return new PageDomain(pageNum, pageSize);
    }

    public int getPageNum() {
        return pageNum;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getOffset() {
        return (pageNum - 1) * pageSize;
    }

    private static int normalizePageNum(Integer pageNum) {
        if (pageNum == null || pageNum < 1) {
            return DEFAULT_PAGE_NUM;
        }
        return pageNum;
    }

    private static int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return pageSize;
    }
}
