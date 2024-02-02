package com.cat2bug.system.domain.vo;

import lombok.Data;

import java.util.List;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-02-02 20:53
 * @Version: 1.0.0
 */
@Data
public class ExcelImportRowResultVo {
    private int rowNum;
    private List<String> messages;
}
