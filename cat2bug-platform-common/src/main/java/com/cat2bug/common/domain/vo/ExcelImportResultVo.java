package com.cat2bug.common.domain.vo;

import java.util.List;

public class ExcelImportResultVo {

    private String message;
    private List<ExcelImportRowResultVo> rows;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<ExcelImportRowResultVo> getRows() {
        return rows;
    }

    public void setRows(List<ExcelImportRowResultVo> rows) {
        this.rows = rows;
    }
}
