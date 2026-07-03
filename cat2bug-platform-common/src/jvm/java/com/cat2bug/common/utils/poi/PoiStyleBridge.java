package com.cat2bug.common.utils.poi;

import com.cat2bug.common.enums.ExcelHorizontalAlign;
import com.cat2bug.common.enums.ExcelIndexedColor;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;

/**
 * JVM ExcelUtil 专用：将无 POI 注解枚举映射为 POI 样式常量。
 */
final class PoiStyleBridge
{
    private PoiStyleBridge()
    {
    }

    static short colorIndex(ExcelIndexedColor color)
    {
        return IndexedColors.valueOf(color.name()).getIndex();
    }

    static HorizontalAlignment alignment(ExcelHorizontalAlign align)
    {
        return HorizontalAlignment.valueOf(align.name());
    }
}
