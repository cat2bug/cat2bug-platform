package com.cat2bug.system.domain;

import com.cat2bug.common.core.domain.entity.SysDefect;
import com.cat2bug.common.utils.bean.BeanUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-07-14 20:09
 * @Version: 1.0.0
 */
@EqualsAndHashCode(callSuper = false)
@Data
public class SysDefectNotice extends SysDefect {
    private String srcHost;

    public SysDefectNotice(String srcHost, SysDefect sysDefect) {
        this.srcHost = srcHost;
        BeanUtils.copyBeanProp(this,sysDefect);
    }
}
