package com.cat2bug.system.domain.vo;

import lombok.Data;

import java.util.List;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-02-02 21:07
 * @Version: 1.0.0
 */
@Data
public class ExcelImportResultVo {
    private String message;
    private List<ExcelImportRowResultVo> rows;
}
