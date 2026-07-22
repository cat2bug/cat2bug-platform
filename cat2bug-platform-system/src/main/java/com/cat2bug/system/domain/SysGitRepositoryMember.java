package com.cat2bug.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.cat2bug.common.core.domain.BaseEntity;

/**
 * 仓库成员 ACL 对象 sys_git_repository_member
 *
 * @author cat2bug
 */
public class SysGitRepositoryMember extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 成员行 ID */
    private Long memberId;

    /** 仓库 ID */
    private Long repositoryId;

    /** 系统用户 ID */
    private Long userId;

    /** 仓角色：read | write | admin */
    private String role;

    /** 用户名（列表 join，非持久化） */
    private String userName;

    /** 昵称（列表 join，非持久化） */
    private String nickName;

    /** 头像（列表 join，非持久化） */
    private String avatar;

    /** 邮箱（列表 join，非持久化） */
    private String email;

    public Long getMemberId()
    {
        return memberId;
    }

    public void setMemberId(Long memberId)
    {
        this.memberId = memberId;
    }

    public Long getRepositoryId()
    {
        return repositoryId;
    }

    public void setRepositoryId(Long repositoryId)
    {
        this.repositoryId = repositoryId;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getRole()
    {
        return role;
    }

    public void setRole(String role)
    {
        this.role = role;
    }

    public String getUserName()
    {
        return userName;
    }

    public void setUserName(String userName)
    {
        this.userName = userName;
    }

    public String getNickName()
    {
        return nickName;
    }

    public void setNickName(String nickName)
    {
        this.nickName = nickName;
    }

    public String getAvatar()
    {
        return avatar;
    }

    public void setAvatar(String avatar)
    {
        this.avatar = avatar;
    }

    public String getEmail()
    {
        return email;
    }

    public void setEmail(String email)
    {
        this.email = email;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("memberId", getMemberId())
            .append("repositoryId", getRepositoryId())
            .append("userId", getUserId())
            .append("role", getRole())
            .append("userName", getUserName())
            .append("nickName", getNickName())
            .append("email", getEmail())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
