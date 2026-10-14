package com.cat2bug.common.domain.open;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

/**
 * Open API 缺陷请求，对齐 Spring {@code com.cat2bug.api.domain.ApiDefectRequest}。
 */
public class ApiDefectRequest extends ApiDefect {

    @JsonIgnore
    private List<Long> handleBy;
    private List<String> handlerAccountList;

    public List<Long> getHandleBy() {
        return handleBy;
    }

    public void setHandleBy(List<Long> handleBy) {
        this.handleBy = handleBy;
    }

    public List<String> getHandlerAccountList() {
        return handlerAccountList;
    }

    public void setHandlerAccountList(List<String> handlerAccountList) {
        this.handlerAccountList = handlerAccountList;
    }
}
