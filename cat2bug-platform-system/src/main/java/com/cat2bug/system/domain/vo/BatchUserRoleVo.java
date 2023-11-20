package com.cat2bug.system.domain.vo;

import lombok.Data;

/**
 * @Author: yuzhantao
 * @CreateTime: 2023-11-21 00:54
 * @Version: 1.0.0
 */
@Data
public class BatchUserRoleVo {
    /**
     * 团队id
     */
    private Long teamId;

    /**
     * 角色id集合
     */
    private Long[] roleIds;

    /**
     * 成员id集合
     */
    private Long[] memberIds;
}
