package com.cat2bug.common.utils.poi;

import org.apache.poi.ss.usermodel.Sheet;

/**
 * Excel监听
 */
public interface IExcelListener {
    public Integer sheetCreated(Sheet sheet, int currentRowNum);
}
