package com.cat2bug.system.domain;

import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.cat2bug.common.annotation.Excel;
import com.cat2bug.common.core.domain.BaseEntity;

/**
 * 用户配置对象 sys_user_config
 * 
 * @author yuzhantao
 * @date 2023-11-16
 */
@Data
public class SysUserConfig extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 用户配置id */
    private Long userConfigId;

    /** 当前团队id */
    private Long currentTeamId;

    /** 当前团队图标 */
    private String currentTeamIcon;

    /** 当前团队名称 */
    private String currentTeamName;

    /** 当前项目id */
    private Long currentProjectId;

    /** 当前项目名称 */
    private String currentProjectName;

    /** 用户id */
    private Long userId;

    /** 人生格言 */
    private String lifeContent;
    /** 禁用或解锁团队 */
    private boolean currentTeamLock;
    /** 禁用或解锁团队原因 */
    private String currentTeamLockRemark;

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("userConfigId", getUserConfigId())
            .append("currentTeamId", getCurrentTeamId())
            .append("userId", getUserId())
            .toString();
    }
}
