package com.cat2bug.quarkus.common.domain.open;

import java.util.List;

/**
 * Open API 缺陷工作流操作，对齐 Spring {@code com.cat2bug.api.domain.ApiDefectHandle}。
 */
public class ApiDefectHandle {

    private Long defectNum;
    private String describe;
    private List<String> handlerAccountList;

    public Long getDefectNum() {
        return defectNum;
    }

    public void setDefectNum(Long defectNum) {
        this.defectNum = defectNum;
    }

    public String getDescribe() {
        return describe;
    }

    public void setDescribe(String describe) {
        this.describe = describe;
    }

    public List<String> getHandlerAccountList() {
        return handlerAccountList;
    }

    public void setHandlerAccountList(List<String> handlerAccountList) {
        this.handlerAccountList = handlerAccountList;
    }
}
