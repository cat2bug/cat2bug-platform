package com.cat2bug.quarkus.common.domain;

import java.io.Serializable;

/**
 * 成员编辑焦点（与 Spring {@code MemberFocus} 对齐）。
 */
public class MemberFocus implements Serializable {

    private String moduleName;
    private Long dataId;
    private SysUser user;

    public MemberFocus() {
    }

    public MemberFocus(String moduleName, Long dataId, SysUser user) {
        this.moduleName = moduleName;
        this.dataId = dataId;
        this.user = user;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public Long getDataId() {
        return dataId;
    }

    public void setDataId(Long dataId) {
        this.dataId = dataId;
    }

    public SysUser getUser() {
        return user;
    }

    public void setUser(SysUser user) {
        this.user = user;
    }
}
