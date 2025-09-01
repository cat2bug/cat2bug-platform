package com.cat2bug.api.domain;

import com.cat2bug.common.annotation.Excel;
import lombok.Data;

import java.util.List;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-03-01 21:44
 * @Version: 1.0.0
 */
@Data
public class ApiMember extends ApiMemberBaseInfo {
    /**
     * 在项目中的权限
     */
    private List<String> roleNameList;
}
