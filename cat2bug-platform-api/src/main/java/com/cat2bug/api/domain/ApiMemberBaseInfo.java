package com.cat2bug.api.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.List;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-03-01 21:44
 * @Version: 1.0.0
 */
@Data
public class ApiMemberBaseInfo {
    private static final long serialVersionUID = 1L;

    /** 成员ID */
    @JsonIgnore
    private Long memberId;
    /** 成员账户 */
    private String memberAccount;
//    /** 用户账号 */
//    @Excel(name = "登录名称")
//    private String userName;
//
//    /** 用户昵称 */
//    @Excel(name = "用户名称")
//    private String nickName;

    /**
     * 成员名称
     */
    private String memberName;

    /** 用户邮箱 */
//    @Excel(name = "用户邮箱")
    private String email;

    /** 手机号码 */
//    @Excel(name = "手机号码")
    private String phoneNumber;

    /** 用户头像 */
    private String avatar;
}
