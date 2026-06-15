package com.cat2bug.common.utils.poi;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

/**
 * Excel 序列日期转 Java {@link Date}（无 POI 依赖，供 ReflectUtils 等共用）。
 */
public final class ExcelSerialDateUtils
{
    private static final LocalDate EXCEL_EPOCH = LocalDate.of(1899, 12, 30);

    private ExcelSerialDateUtils()
    {
    }

    public static Date toJavaDate(double serial)
    {
        long wholeDays = (long) Math.floor(serial);
        double fraction = serial - wholeDays;
        LocalDate date = EXCEL_EPOCH.plusDays(wholeDays);
        long millisOfDay = Math.round(fraction * 24 * 60 * 60 * 1000);
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant().plusMillis(millisOfDay));
    }
}
