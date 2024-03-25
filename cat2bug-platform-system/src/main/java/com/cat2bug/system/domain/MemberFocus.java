package com.cat2bug.system.domain;

import com.cat2bug.common.core.domain.entity.SysUser;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-03-22 00:04
 * @Version: 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberFocus {
    private String moduleName;
    private Long dataId;
    private SysUser user;
}
