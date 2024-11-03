package com.cat2bug.api.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.List;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-11-03 21:01
 * @Version: 1.0.0
 */
@Data
public class ApiDefectRequest extends ApiDefect {
    /** 处理人ID集合 */
    @JsonIgnore
    private List<Long> handleBy;

    /** 处理人账号集合 */
    private List<String> handlerAccountList;
}
