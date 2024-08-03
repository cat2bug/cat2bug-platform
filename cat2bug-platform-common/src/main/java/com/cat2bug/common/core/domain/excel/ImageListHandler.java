package com.cat2bug.common.core.domain.excel;

import com.cat2bug.common.config.Cat2BugConfig;
import com.cat2bug.common.utils.file.ImageUtils;
import com.cat2bug.common.utils.poi.ExcelHandlerAdapter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.util.IOUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-06-07 21:58
 * @Version: 1.0.0
 */
public class ImageListHandler implements ExcelHandlerAdapter {
    @Override
    public Object format(Object value, String[] args, Cell cell, Workbook wb, Map<String, Object> requestParams) {
        String v = (String)value;
        if(v==null || "".equals(v)) {
            return "";
        }
        Drawing drawing = wb.getSheetAt(0).createDrawingPatriarch();
        String[] strImgs = v.split(",");
        return IntStream.range(0,strImgs.length).mapToObj(i->{
            try {
                String filePath = Cat2BugConfig.getUploadPath();

                String path = strImgs[i];

                if(path==null || "".equals(path)) return null;

                byte[] bytes = ImageUtils.getImage(path);
                // 在单元格内部定位图片
                ClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, i, i, i + 1, i + 1);
                // 插入图片
                Picture picture = drawing.createPicture(anchor, wb.addPicture(bytes, Workbook.PICTURE_TYPE_JPEG));
                return picture;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).filter(img->img!=null).collect(Collectors.toList());
    }
}
