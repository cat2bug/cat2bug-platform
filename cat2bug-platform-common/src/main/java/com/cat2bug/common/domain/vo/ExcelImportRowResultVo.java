package com.cat2bug.common.domain.vo;

import java.util.List;

public class ExcelImportRowResultVo {

    private Integer rowNum;
    private Integer rowCount = 1;
    private List<String> messages;

    public Integer getRowNum() {
        return rowNum;
    }

    public void setRowNum(Integer rowNum) {
        this.rowNum = rowNum;
    }

    public Integer getRowCount() {
        return rowCount;
    }

    public void setRowCount(Integer rowCount) {
        this.rowCount = rowCount;
    }

    public List<String> getMessages() {
        return messages;
    }

    public void setMessages(List<String> messages) {
        this.messages = messages;
    }
}
