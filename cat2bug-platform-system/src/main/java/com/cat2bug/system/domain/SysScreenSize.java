package com.cat2bug.system.domain;

import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.cat2bug.common.annotation.Excel;
import com.cat2bug.common.core.domain.BaseEntity;

/**
 * 屏幕尺寸对象 sys_screen_size
 * 
 * @author yuzhantao
 * @date 2023-12-10
 */
@Data
public class SysScreenSize extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 屏幕尺寸id */
    private Long screenSizeId;

    /** 名称 */
    @Excel(name = "名称")
    private String name;

    /** 宽 */
    @Excel(name = "宽")
    private String width;

    /** 高 */
    @Excel(name = "高")
    private String height;

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("screenSizeId", getScreenSizeId())
            .append("name", getName())
            .append("width", getWidth())
            .append("height", getHeight())
            .append("remark", getRemark())
            .toString();
    }
}
