package com.cat2bug.api.domain;

import com.cat2bug.common.annotation.Excel;
import lombok.Data;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-03-01 21:44
 * @Version: 1.0.0
 */
@Data
public class ApiMember {
    private static final long serialVersionUID = 1L;

    /** 用户ID */
    @Excel(name = "用户序号", cellType = Excel.ColumnType.NUMERIC, prompt = "用户编号")
    private Long memberId;

    /** 用户账号 */
    @Excel(name = "登录名称")
    private String userName;

    /** 用户昵称 */
    @Excel(name = "用户名称")
    private String nickName;

    /** 用户邮箱 */
    @Excel(name = "用户邮箱")
    private String email;

    /** 手机号码 */
    @Excel(name = "手机号码")
    private String phoneNumber;

    /** 用户头像 */
    private String avatar;
}
