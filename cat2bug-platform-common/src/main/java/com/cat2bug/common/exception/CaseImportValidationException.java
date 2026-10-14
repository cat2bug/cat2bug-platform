package com.cat2bug.common.exception;

import com.cat2bug.common.domain.vo.ExcelImportResultVo;

/**
 * 用例导入校验失败：触发事务回滚，由 Resource 转为 AjaxResult.success(data)。
 */
public class CaseImportValidationException extends RuntimeException {

    private final ExcelImportResultVo result;

    public CaseImportValidationException(ExcelImportResultVo result) {
        super(result != null ? result.getMessage() : "导入失败");
        this.result = result;
    }

    public ExcelImportResultVo getResult() {
        return result;
    }
}
