package com.cat2bug.quarkus.common.api;

import java.util.List;

/**
 * 与 Spring 版 TableDataInfo 字段形态兼容。
 */
public class TableDataInfo {

    private long total;
    private List<?> rows;
    private int code = AjaxResult.HTTP_OK;
    private String msg = "查询成功";

    public TableDataInfo() {
    }

    public TableDataInfo(List<?> rows, long total) {
        this.rows = rows;
        this.total = total;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public List<?> getRows() {
        return rows;
    }

    public void setRows(List<?> rows) {
        this.rows = rows;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
