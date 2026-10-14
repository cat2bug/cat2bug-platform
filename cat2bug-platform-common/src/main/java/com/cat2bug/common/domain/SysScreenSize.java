package com.cat2bug.common.domain;

/**
 * 屏幕尺寸对象 sys_screen_size
 */
public class SysScreenSize {

    private Long screenSizeId;
    private String name;
    private String width;
    private String height;
    private String remark;

    public Long getScreenSizeId() {
        return screenSizeId;
    }

    public void setScreenSizeId(Long screenSizeId) {
        this.screenSizeId = screenSizeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getWidth() {
        return width;
    }

    public void setWidth(String width) {
        this.width = width;
    }

    public String getHeight() {
        return height;
    }

    public void setHeight(String height) {
        this.height = height;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
