package com.cat2bug.common.utils.poi;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.*;

import com.cat2bug.common.utils.MessageUtils;
import com.cat2bug.common.utils.file.IFileService;
import com.cat2bug.common.utils.spring.SpringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.hssf.usermodel.HSSFClientAnchor;
import org.apache.poi.hssf.usermodel.HSSFPicture;
import org.apache.poi.hssf.usermodel.HSSFPictureData;
import org.apache.poi.hssf.usermodel.HSSFShape;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ooxml.POIXMLDocumentPart;
import org.apache.poi.ooxml.util.PackageHelper;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.openxml4j.opc.PackagePartName;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.Units;
import org.apache.poi.xddf.usermodel.*;
import org.apache.poi.xddf.usermodel.chart.XDDFChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFLineChartData;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.*;

import org.openxmlformats.schemas.drawingml.x2006.chart.*;
import com.cat2bug.common.annotation.Excel;
import com.cat2bug.common.annotation.Excel.ColumnType;
import com.cat2bug.common.annotation.Excel.Type;
import com.cat2bug.common.annotation.Excels;
import com.cat2bug.common.config.Cat2BugConfig;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.core.text.Convert;
import com.cat2bug.common.exception.UtilException;
import com.cat2bug.common.utils.DateUtils;
import com.cat2bug.common.utils.DictUtils;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.common.utils.file.FileTypeUtils;
import com.cat2bug.common.utils.file.ImageUtils;
import com.cat2bug.common.utils.reflect.ReflectUtils;
import org.openxmlformats.schemas.drawingml.x2006.main.CTSRgbColor;
import org.openxmlformats.schemas.drawingml.x2006.main.CTShapeProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTSolidColorFillProperties;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Excel相关处理
 * 
 * @author ruoyi
 */
public class ExcelUtil<T>
{
    private static final Logger log = LogManager.getLogger(ExcelUtil.class);

    public static final String FORMULA_REGEX_STR = "=|-|\\+|@";

    public static final String[] FORMULA_STR = { "=", "-", "+", "@" };

    /** ECharts的macarons主题默认颜色数组 */
    private static final byte[][] ECHARTS_COLORS = {
            new byte[] {(byte)0x2e, (byte)0xc7, (byte)0xc9},
            new byte[] {(byte)0xb6, (byte)0xa2, (byte)0xde},
            new byte[] {(byte)0x5a, (byte)0xb1, (byte)0xef},
            new byte[] {(byte)0xff, (byte)0xb9, (byte)0x80},
            new byte[] {(byte)0xd8, 0x7a, (byte)0x80},
            new byte[] {(byte)0x8d, (byte)0x98, (byte)0xb3},
            new byte[] {(byte)0xe5, (byte)0xcf, 0x0d},
            new byte[] {(byte)0x97, (byte)0xb5, 0x52},
            new byte[] {(byte)0x95, 0x70, 0x6d},
            new byte[] {(byte)0xdc, 0x69, (byte)0xaa},
            new byte[] {(byte)0x07, (byte)0xa2, (byte)0xa4},
            new byte[] {(byte)0x9a, 0x7f, (byte)0xd1},
            new byte[] {(byte)0x58, (byte)0x8d, (byte)0xd5},
            new byte[] {(byte)0xf5, (byte)0x99, 0x4e},
            new byte[] {(byte)0xc0, 0x50, 0x50},
            new byte[] {(byte)0x59, 0x67, (byte)0x8c},
            new byte[] {(byte)0xc9, (byte)0xab, 0x0},
            new byte[] {(byte)0xc7e, (byte)0xb0, 0x0a},
            new byte[] {(byte)0x6f, 0x55, 0x53},
            new byte[] {(byte)0xcc1, 0x40, (byte)0x89}
    };

    /**
     * 用于dictType属性数据存储，避免重复查缓存
     */
    public Map<String, String> sysDictMap = new HashMap<String, String>();

    /**
     * Excel sheet最大行数，默认65536
     */
    public static final int sheetSize = 65536;

    /**
     * 工作表名称
     */
    private String sheetName;

    /**
     * 导出类型（EXPORT:导出数据；IMPORT：导入模板）
     */
    private Type type;

    /**
     * 工作薄对象
     */
    private Workbook wb;

    /**
     * 工作表对象
     */
    private Sheet sheet;

    /**
     * 样式列表
     */
    private Map<String, CellStyle> styles;

    /**
     * 导入导出数据列表
     */
    private List<T> list;

    /**
     * 注解列表
     */
    private List<Object[]> fields;

    /**
     * 当前行号
     */
    private int rownum;

    /**
     * 标题
     */
    private String title;

    /**
     * 最大高度
     */
    private short maxHeight;

    /**
     * 合并后最后行数
     */
    private int subMergedLastRowNum = 0;

    /**
     * 合并后开始行数
     */
    private int subMergedFirstRowNum = 1;

    /**
     * 对象的子列表方法
     */
    private Method subMethod;

    /**
     * 对象的子列表属性
     */
    private List<Field> subFields;
    /**
     * 参数
     */
    private Map<String,Object> params;

    /**
     * 统计列表
     */
    private Map<Integer, Double> statistics = new HashMap<Integer, Double>();

    /**
     * 数字格式
     */
    private static final DecimalFormat DOUBLE_FORMAT = new DecimalFormat("######0.00");

    /**
     * Excel监听回调
     */
    private IExcelListener excelListener;

    /**
     * 实体对象
     */
    public Class<T> clazz;

    /**
     * 需要排除列属性
     */
    public String[] excludeFields;

    public ExcelUtil(Class<T> clazz)
    {
        this.clazz = clazz;
    }

    public ExcelUtil(Class<T> clazz, Map<String, Object> params)
    {
        this.clazz = clazz;
        this.params = params;
    }

    /**
     * 创建折线图表时应用颜色
     * @param data
     */
    public static void applyLineChartsColors(XSSFChart chart, XDDFChartData data) {
        List<XDDFChartData.Series> seriesList = data.getSeries();
        for (int i = 0; i < seriesList.size(); i++) {
            setSeriesColor(chart, i,  seriesList.get(i), ECHARTS_COLORS[i % ECHARTS_COLORS.length]);
        }
    }

    /**
     * 为系列设置颜色
     * @param series
     * @param hexColor
     */
    private static void setSeriesColor(XSSFChart chart, int lineIndex, XDDFChartData.Series series,
                                       byte[] hexColor) {
        // 将十六进制颜色转换为POI可用的XDDFColor
        XDDFColor color = XDDFColor.from(hexColor);

        // 设置线条的颜色
        XDDFSolidFillProperties fill = new XDDFSolidFillProperties(color);
        XDDFLineProperties line = new XDDFLineProperties();
        line.setFillProperties(fill);
        line.setWidth(2.0); // 设置线宽，单位磅，与ECharts默认接近
        // 获取或创建形状属性
        XDDFShapeProperties properties = series.getShapeProperties();
        if (properties == null) {
            properties = new XDDFShapeProperties();
        }
        properties.setLineProperties(line);
        properties.setFillProperties(fill);
        series.setShapeProperties(properties);

        // 设置标记点的颜色
        XDDFSolidFillProperties fillMarker = new XDDFSolidFillProperties(color);
        XDDFLineProperties markerLine = new XDDFLineProperties();
        markerLine.setFillProperties(fill);
        markerLine.setWidth(0.0);
        XDDFShapeProperties propertiesMarker = new XDDFShapeProperties();
        propertiesMarker.setFillProperties(fillMarker);
        propertiesMarker.setLineProperties(markerLine);
        // 获取区域
        CTPlotArea ctPlotArea = chart.getCTChart().getPlotArea();
        // 获取折线区域集合
        CTLineChart[] ctLineCharts =ctPlotArea.getLineChartArray();
        if(ctLineCharts.length>0) {
            // 获取折线
            CTLineSer[] lines = ctLineCharts[0].getSerArray();
            // 获取折线的标记点对象
            CTMarker ctMarker = lines[lineIndex].getMarker();
            // 获取标记点属性
            CTShapeProperties markerShapeProperties = ctMarker.addNewSpPr();
            markerShapeProperties.set(propertiesMarker.getXmlObject());
        }
    }

    /**
     * 隐藏Excel中列属性
     *
     * @param fields 列属性名 示例[单个"name"/多个"id","name"]
     * @throws Exception
     */
    public void hideColumn(String... fields)
    {
        this.excludeFields = fields;
    }

    public void init(List<T> list, String sheetName, String title, Type type, Map<String,Object> params, IExcelListener excelListener )
    {
        if (list == null)
        {
            list = new ArrayList<T>();
        }
        this.list = list;
        this.sheetName = sheetName;
        this.type = type;
        this.title = title;
        this.params = params;
        this.excelListener = excelListener;
        createExcelField();
        createWorkbook();
        createTitle();
        createSubHead();
    }

    /**
     * 创建excel第一行标题
     */
    public void createTitle()
    {
        if (StringUtils.isNotEmpty(title))
        {
            subMergedFirstRowNum++;
            subMergedLastRowNum++;
            int titleLastCol = this.fields.size() - 1;
            if (isSubList())
            {
                titleLastCol = titleLastCol + subFields.size() - 1;
            }
            Row titleRow = sheet.createRow(rownum == 0 ? rownum++ : 0);
            titleRow.setHeightInPoints(30);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellStyle(styles.get("title"));
            titleCell.setCellValue(title);
            sheet.addMergedRegion(new CellRangeAddress(titleRow.getRowNum(), titleRow.getRowNum(), titleRow.getRowNum(), titleLastCol));
        }
    }

    /**
     * 创建对象的子列表名称
     */
    public void createSubHead()
    {
        if (isSubList())
        {
            subMergedFirstRowNum++;
            subMergedLastRowNum++;
            Row subRow = sheet.createRow(rownum);
            int excelNum = 0;
            for (Object[] objects : fields)
            {
                Excel attr = (Excel) objects[1];
                Cell headCell1 = subRow.createCell(excelNum);
                headCell1.setCellValue(getTitleName(attr));
                headCell1.setCellStyle(styles.get(StringUtils.format("header_{}_{}", attr.headerColor(), attr.headerBackgroundColor())));
                excelNum++;
            }
            int headFirstRow = excelNum - 1;
            int headLastRow = headFirstRow + subFields.size() - 1;
            if (headLastRow > headFirstRow)
            {
                sheet.addMergedRegion(new CellRangeAddress(rownum, rownum, headFirstRow, headLastRow));
            }
            rownum++;
        }
    }

    private String getTitleName(Excel excel) {
        return StringUtils.isNotEmpty(excel.i18nNameKey()) && StringUtils.isNotEmpty(MessageUtils.message(excel.i18nNameKey()))?
                MessageUtils.message(excel.i18nNameKey()):
                excel.name();
    }

    /**
     * 对excel表单默认第一个索引名转换成list
     * 
     * @param is 输入流
     * @return 转换后集合
     */
    public List<T> importExcel(InputStream is, Object... params)
    {
        List<T> list = null;
        try
        {
            list = importExcel(is, 0);
        }
        catch (Exception e)
        {
            log.error(e);
        }
        finally
        {
            IOUtils.closeQuietly(is);
        }
        return list;
    }

    /**
     * 对excel表单默认第一个索引名转换成list
     * 
     * @param is 输入流
     * @param titleNum 标题占用行数
     * @return 转换后集合
     */
    public List<T> importExcel(InputStream is, int titleNum) throws Exception
    {
        return importExcel(StringUtils.EMPTY, is, titleNum);
    }

    /**
     * 对excel表单指定表格索引名转换成list
     * 
     * @param sheetName 表格索引名
     * @param titleNum 标题占用行数
     * @param inputStream 输入流
     * @return 转换后集合
     */
    public List<T> importExcel(String sheetName, InputStream inputStream, int titleNum) throws Exception
    {
        this.type = Type.IMPORT;
        //将InputStream对象转换成ByteArrayOutputStream
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = inputStream.read(buffer)) > -1 ) {
            byteArrayOutputStream.write(buffer, 0, len);
        }
        byteArrayOutputStream.flush();
        //将byteArrayOutputStream可转换成多个InputStream对象，达到多次读取InputStream效果
        InputStream is = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        this.wb = WorkbookFactory.create(is);
        List<T> list = new ArrayList<T>();
        // 如果指定sheet名,则取指定sheet中的内容 否则默认指向第1个sheet
        Sheet sheet = StringUtils.isNotEmpty(sheetName) ? wb.getSheet(sheetName) : wb.getSheetAt(0);
        if (sheet == null)
        {
            throw new IOException("文件sheet不存在");
        }
        boolean isXSSFWorkbook = !(wb instanceof HSSFWorkbook);
        // 获取悬浮图片
        Map<String, List<PictureData>> pictures;
        if (isXSSFWorkbook)
        {
            pictures = getSheetPictures07((XSSFSheet) sheet, (XSSFWorkbook) wb);
        }
        else
        {
            pictures = getSheetPictures03((HSSFSheet) sheet, (HSSFWorkbook) wb);
        }

        // 获取嵌入式图片
        InputStream embedIs = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        Map<String, PackagePart> embedPictures = getEmbedPictureMap(embedIs);

        // 获取最后一个非空行的行下标，比如总行数为n，则返回的为n-1
        int rows = sheet.getLastRowNum();
        if (rows > 0)
        {
            // 定义一个map用于存放excel列的序号和field.
            Map<String, Integer> cellMap = new HashMap<String, Integer>();
            // 获取表头
            Row heard = sheet.getRow(titleNum);
            for (int i = 0; i < heard.getPhysicalNumberOfCells(); i++)
            {
                Cell cell = heard.getCell(i);
                if (StringUtils.isNotNull(cell))
                {
                    String value = this.getCellValue(heard, i).toString();
                    cellMap.put(value, i);
                }
                else
                {
                    cellMap.put(null, i);
                }
            }
            // 有数据时才处理 得到类的所有field.
            List<Object[]> fields = this.getFields();
            Map<Integer, Object[]> fieldsMap = new HashMap<Integer, Object[]>();
            for (Object[] objects : fields)
            {
                Excel attr = (Excel) objects[1];
                Integer column = cellMap.get(this.getTitleName(attr));
                if (column != null)
                {
                    fieldsMap.put(column, objects);
                }
            }
            for (int i = titleNum + 1; i <= rows; i++)
            {
                // 从第2行开始取数据,默认第一行是表头.
                Row row = sheet.getRow(i);
                // 判断当前行是否是空行
                if (isRowEmpty(row))
                {
                    continue;
                }
                T entity = null;
                for (Map.Entry<Integer, Object[]> entry : fieldsMap.entrySet())
                {
                    Object val = this.getCellValue(row, entry.getKey());

                    // 如果不存在实例则新建.
                    entity = (entity == null ? clazz.newInstance() : entity);
                    // 从map中得到对应列的field.
                    Field field = (Field) entry.getValue()[0];
                    Excel attr = (Excel) entry.getValue()[1];
                    // 取得类型,并根据对象类型设置值.
                    Class<?> fieldType = field.getType();
                    if (String.class == fieldType)
                    {
                        String s = Convert.toStr(val);
                        if (StringUtils.endsWith(s, ".0"))
                        {
                            val = StringUtils.substringBefore(s, ".0");
                        }
                        else
                        {
                            String dateFormat = field.getAnnotation(Excel.class).dateFormat();
                            if (StringUtils.isNotEmpty(dateFormat))
                            {
                                val = parseDateToStr(dateFormat, val);
                            }
                            else
                            {
                                val = Convert.toStr(val);
                            }
                        }
                    }
                    else if ((Integer.TYPE == fieldType || Integer.class == fieldType) && StringUtils.isNumeric(Convert.toStr(val)))
                    {
                        val = Convert.toInt(val);
                    }
                    else if ((Long.TYPE == fieldType || Long.class == fieldType) && StringUtils.isNumeric(Convert.toStr(val)))
                    {
                        val = Convert.toLong(val);
                    }
                    else if (Double.TYPE == fieldType || Double.class == fieldType)
                    {
                        val = Convert.toDouble(val);
                    }
                    else if (Float.TYPE == fieldType || Float.class == fieldType)
                    {
                        val = Convert.toFloat(val);
                    }
                    else if (BigDecimal.class == fieldType)
                    {
                        val = Convert.toBigDecimal(val);
                    }
                    else if (Date.class == fieldType)
                    {
                        if (val instanceof String)
                        {
                            val = DateUtils.parseDate(val);
                        }
                        else if (val instanceof Double)
                        {
                            val = DateUtil.getJavaDate((Double) val);
                        }
                    }
                    else if (Boolean.TYPE == fieldType || Boolean.class == fieldType)
                    {
                        val = Convert.toBool(val, false);
                    }
                    if (StringUtils.isNotNull(fieldType))
                    {
                        String propertyName = field.getName();
                        if (StringUtils.isNotEmpty(attr.targetAttr()))
                        {
                            propertyName = field.getName() + "." + attr.targetAttr();
                        }
                        if (StringUtils.isNotEmpty(attr.readConverterExp()))
                        {
                            val = reverseByExp(Convert.toStr(val), attr.readConverterExp(), attr.separator());
                        }
                        else if (StringUtils.isNotEmpty(attr.dictType()))
                        {
                            val = reverseDictByExp(Convert.toStr(val), attr.dictType(), attr.separator());
                        }
                        else if (!attr.handler().equals(ExcelHandlerAdapter.class))
                        {
                            val = dataFormatHandlerAdapter(val, attr, null);
                        }
                        else if (ColumnType.IMAGE_LIST == attr.cellType())
                        {
                            List<String> imgPathList = new ArrayList<>();
                            String key = row.getRowNum() + "_" + entry.getKey();
                            if(StringUtils.isNotEmpty(pictures) && pictures.containsKey(key)) {
                                List<PictureData> images = pictures.get(key);
                                if (images != null && images.size()>0) {
                                    imgPathList.addAll(images.stream().map(img->{
                                        try {
                                            byte[] data = img.getData();
                                            IFileService fileService = SpringUtils.getBean(IFileService.class);
                                            return fileService.uploadImportBytes(data, true);
                                        } catch (IOException e) {
                                            throw new RuntimeException(e);
                                        }
                                    }).collect(Collectors.toList()));
                                }
                            }
                            // 如果返回的对象为空，通过查看嵌入图片方式看看是否可以查找到图片
                            if(embedPictures!=null && embedPictures.size()>0) {
                                Cell cell = row.getCell(entry.getKey());
                                if (cell != null) {
                                    String embedImgPath = writeEmbedImage(embedPictures, cell);
                                    if (StringUtils.isNotBlank(embedImgPath)) {
                                        imgPathList.add(embedImgPath);
                                    }
                                }
                            }
                            if(imgPathList.size()>0) {
                                val = imgPathList.stream().collect(Collectors.joining(","));
                            } else {
                                val = "";
                            }
                        }
                        ReflectUtils.invokeSetter(entity, propertyName, val);
                    }
                }
                list.add(entity);
            }
        }
        embedIs.close();
        is.close();
        return list;
    }

    /**
     * 对list数据源将其里面的数据导入到excel表单
     * 
     * @param list 导出数据集合
     * @param sheetName 工作表的名称
     * @return 结果
     */
    public AjaxResult exportExcel(List<T> list, String sheetName)
    {
        return exportExcel(list, sheetName, StringUtils.EMPTY);
    }

    /**
     * 对list数据源将其里面的数据导入到excel表单
     * 
     * @param list 导出数据集合
     * @param sheetName 工作表的名称
     * @param title 标题
     * @return 结果
     */
    public AjaxResult exportExcel(List<T> list, String sheetName, String title)
    {
        this.init(list, sheetName, title, Type.EXPORT, new HashMap<>(), null);
        return exportExcel();
    }

    /**
     * 对list数据源将其里面的数据导入到excel表单
     * 
     * @param response 返回数据
     * @param list 导出数据集合
     * @param sheetName 工作表的名称
     * @return 结果
     */
    public void exportExcel(HttpServletResponse response, List<T> list, String sheetName)
    {
        exportExcel(response, list, sheetName, StringUtils.EMPTY);
    }

    /**
     * 对list数据源将其里面的数据导入到excel表单
     *
     * @param response 返回数据
     * @param list 导出数据集合
     * @param sheetName 工作表的名称
     * @return 结果
     */
    public void exportExcel(HttpServletResponse response, List<T> list, String sheetName, Map<String, Object> params)
    {
        exportExcel(response, list, sheetName, StringUtils.EMPTY,params);
    }

    /**
     * 对list数据源将其里面的数据导入到excel表单
     * @param response 返回数据
     * @param list 导出数据集合
     * @param sheetName 工作表的名称
     * @param title 标题
     * @return 结果
     */
    public void exportExcel(HttpServletResponse response, List<T> list, String sheetName, String title, Map<String, Object> params)
    {
        exportExcel(response, list, sheetName, title, params, null);
    }

    /**
     * 对list数据源将其里面的数据导入到excel表单
     * @param response 返回数据
     * @param list 导出数据集合
     * @param sheetName 工作表的名称
     * @param title 标题
     * @param params
     * @param listener
     */
    public void exportExcel(HttpServletResponse response, List<T> list, String sheetName, String title, Map<String, Object> params, IExcelListener listener)
    {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        this.init(list, sheetName, title, Type.EXPORT,params, listener);
        exportExcel(response);
    }

    /**
     * 对list数据源将其里面的数据导入到excel表单
     *
     * @param response 返回数据
     * @param list 导出数据集合
     * @param sheetName 工作表的名称
     * @param title 标题
     * @return 结果
     */
    public void exportExcel(HttpServletResponse response, List<T> list, String sheetName, String title)
    {
        this.exportExcel(response,list,sheetName,title,new HashMap<>());
    }

    /**
     * 对list数据源将其里面的数据导入到excel表单
     * 
     * @param sheetName 工作表的名称
     * @return 结果
     */
    public AjaxResult importTemplateExcel(String sheetName)
    {
        return importTemplateExcel(sheetName, StringUtils.EMPTY);
    }

    /**
     * 对list数据源将其里面的数据导入到excel表单
     * 
     * @param sheetName 工作表的名称
     * @param title 标题
     * @return 结果
     */
    public AjaxResult importTemplateExcel(String sheetName, String title)
    {
        this.init(null, sheetName, title, Type.IMPORT, new HashMap<String,Object>(),null);
        return exportExcel();
    }

    /**
     * 对list数据源将其里面的数据导入到excel表单
     * 
     * @param sheetName 工作表的名称
     * @return 结果
     */
    public void importTemplateExcel(HttpServletResponse response, String sheetName)
    {
        importTemplateExcel(response, sheetName, StringUtils.EMPTY, new HashMap<>());
    }

    /**
     * 对list数据源将其里面的数据导入到excel表单
     *
     * @param sheetName 工作表的名称
     * @return 结果
     */
    public void importTemplateExcel(HttpServletResponse response, String sheetName, Map<String,Object> params)
    {
        importTemplateExcel(response, sheetName, StringUtils.EMPTY, params);
    }

    /**
     * 对list数据源将其里面的数据导入到excel表单
     * 
     * @param sheetName 工作表的名称
     * @param title 标题
     * @return 结果
     */
    public void importTemplateExcel(HttpServletResponse response, String sheetName, String title ,Map<String,Object> params)
    {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        this.init(null, sheetName, title, Type.IMPORT,params,null);
        exportExcel(response);
    }

    /**
     * 对list数据源将其里面的数据导入到excel表单
     * 
     * @return 结果
     */
    public void exportExcel(HttpServletResponse response)
    {
        try
        {
            writeSheet();
            wb.write(response.getOutputStream());
        }
        catch (Exception e)
        {
            log.error("导出Excel异常{}", e.getMessage());
        }
        finally
        {
            IOUtils.closeQuietly(wb);
        }
    }

    /**
     * 对list数据源将其里面的数据导入到excel表单
     * 
     * @return 结果
     */
    public AjaxResult exportExcel()
    {
        OutputStream out = null;
        try
        {
            writeSheet();
            String filename = encodingFilename(sheetName);
            out = new FileOutputStream(getAbsoluteFile(filename));
            wb.write(out);
            return AjaxResult.success(filename);
        }
        catch (Exception e)
        {
            log.error("导出Excel异常{}", e.getMessage());
            throw new UtilException("导出Excel失败，请联系网站管理员！");
        }
        finally
        {
            IOUtils.closeQuietly(wb);
            IOUtils.closeQuietly(out);
        }
    }

    /**
     * 创建写入数据到Sheet
     */
    public void writeSheet()
    {
        // 取出一共有多少个sheet.
        int sheetNo = Math.max(1, (int) Math.ceil(list.size() * 1.0 / sheetSize));
        for (int index = 0; index < sheetNo; index++)
        {
            createSheet(sheetNo, index);
            // 产生一行
            Row row = sheet.createRow(rownum);
            int column = 0;
            // 写入各个字段的列头名称
            for (Object[] os : fields)
            {
                Field field = (Field) os[0];
                Excel excel = (Excel) os[1];
                if (Collection.class.isAssignableFrom(field.getType()) && subFields.size()>0)
                {
                    for (Field subField : subFields)
                    {
                        Excel subExcel = subField.getAnnotation(Excel.class);
                        this.createHeadCell(subExcel, row, column++);
                    }
                }
                else
                {
                    this.createHeadCell(excel, row, column++);
                }
            }
            if (Type.EXPORT.equals(type))
            {
                fillExcelData(index, row);
                addStatisticsRow();
            }
        }
    }

    /**
     * 填充excel数据
     * 
     * @param index 序号
     * @param row 单元格行
     */
    @SuppressWarnings("unchecked")
    public void fillExcelData(int index, Row row)
    {
        int startNo = index * sheetSize;
        int endNo = Math.min(startNo + sheetSize, list.size());
        int rowNo = (1 + rownum) - startNo;
        for (int i = startNo; i < endNo; i++)
        {
            rowNo = isSubList() ? (i > 1 ? rowNo + 1 : rowNo + i) : i + 1 + rownum - startNo;
            row = sheet.createRow(rowNo);
            // 得到导出对象.
            T vo = (T) list.get(i);
            Collection<?> subList = null;
            if (isSubList())
            {
                if (isSubListValue(vo))
                {
                    subList = getListCellValue(vo);
                    subMergedLastRowNum = subMergedLastRowNum + subList.size();
                }
                else
                {
                    subMergedFirstRowNum++;
                    subMergedLastRowNum++;
                }
            }
            int column = 0;
            for (Object[] os : fields)
            {
                Field field = (Field) os[0];
                Excel excel = (Excel) os[1];
                if (Collection.class.isAssignableFrom(field.getType()) && StringUtils.isNotNull(subList))
                {
                    boolean subFirst = false;
                    for (Object obj : subList)
                    {
                        if (subFirst)
                        {
                            rowNo++;
                            row = sheet.createRow(rowNo);
                        }
                        List<Field> subFields = FieldUtils.getFieldsListWithAnnotation(obj.getClass(), Excel.class);
                        int subIndex = 0;
                        for (Field subField : subFields)
                        {
                            if (subField.isAnnotationPresent(Excel.class))
                            {
                                subField.setAccessible(true);
                                Excel attr = subField.getAnnotation(Excel.class);
                                this.addCell(attr, row, (T) obj, subField, column + subIndex);
                            }
                            subIndex++;
                        }
                        subFirst = true;
                    }
                    this.subMergedFirstRowNum = this.subMergedFirstRowNum + subList.size();
                }
                else
                {
                    this.addCell(excel, row, vo, field, column++);
                }
            }
        }
    }

    /**
     * 创建表格样式
     * 
     * @param wb 工作薄对象
     * @return 样式列表
     */
    private Map<String, CellStyle> createStyles(Workbook wb)
    {
        // 写入各条记录,每条记录对应excel表中的一行
        Map<String, CellStyle> styles = new HashMap<String, CellStyle>();
        CellStyle style = wb.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        Font titleFont = wb.createFont();
        titleFont.setFontName("Arial");
        titleFont.setFontHeightInPoints((short) 16);
        titleFont.setBold(true);
        style.setFont(titleFont);
        styles.put("title", style);

        style = wb.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderRight(BorderStyle.THIN);
        style.setRightBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
        style.setBorderLeft(BorderStyle.THIN);
        style.setLeftBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
        style.setBorderTop(BorderStyle.THIN);
        style.setTopBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
        style.setBorderBottom(BorderStyle.THIN);
        style.setBottomBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
        Font dataFont = wb.createFont();
        dataFont.setFontName("Arial");
        dataFont.setFontHeightInPoints((short) 10);
        style.setFont(dataFont);
        styles.put("data", style);

        style = wb.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        Font totalFont = wb.createFont();
        totalFont.setFontName("Arial");
        totalFont.setFontHeightInPoints((short) 10);
        style.setFont(totalFont);
        styles.put("total", style);

        styles.putAll(annotationHeaderStyles(wb, styles));

        styles.putAll(annotationDataStyles(wb));

        return styles;
    }

    /**
     * 根据Excel注解创建表格头样式
     * 
     * @param wb 工作薄对象
     * @return 自定义样式列表
     */
    private Map<String, CellStyle> annotationHeaderStyles(Workbook wb, Map<String, CellStyle> styles)
    {
        Map<String, CellStyle> headerStyles = new HashMap<String, CellStyle>();
        for (Object[] os : fields)
        {
            Excel excel = (Excel) os[1];
            String key = StringUtils.format("header_{}_{}", excel.headerColor(), excel.headerBackgroundColor());
            if (!headerStyles.containsKey(key))
            {
                CellStyle style = wb.createCellStyle();
                style.cloneStyleFrom(styles.get("data"));
                style.setAlignment(HorizontalAlignment.CENTER);
                style.setVerticalAlignment(VerticalAlignment.CENTER);
                style.setFillForegroundColor(excel.headerBackgroundColor().index);
                style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                Font headerFont = wb.createFont();
                headerFont.setFontName("Arial");
                headerFont.setFontHeightInPoints((short) 10);
                headerFont.setBold(true);
                headerFont.setColor(excel.headerColor().index);
                style.setFont(headerFont);
                headerStyles.put(key, style);
            }
        }
        return headerStyles;
    }

    /**
     * 根据Excel注解创建表格列样式
     * 
     * @param wb 工作薄对象
     * @return 自定义样式列表
     */
    private Map<String, CellStyle> annotationDataStyles(Workbook wb)
    {
        Map<String, CellStyle> styles = new HashMap<String, CellStyle>();
        for (Object[] os : fields)
        {
            Excel excel = (Excel) os[1];
            String key = StringUtils.format("data_{}_{}_{}", excel.align(), excel.color(), excel.backgroundColor());
            if (!styles.containsKey(key))
            {
                CellStyle style = wb.createCellStyle();
                style.setAlignment(excel.align());
                style.setVerticalAlignment(VerticalAlignment.CENTER);
                style.setBorderRight(BorderStyle.THIN);
                style.setRightBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
                style.setBorderLeft(BorderStyle.THIN);
                style.setLeftBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
                style.setBorderTop(BorderStyle.THIN);
                style.setTopBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
                style.setBorderBottom(BorderStyle.THIN);
                style.setBottomBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
                style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                style.setFillForegroundColor(excel.backgroundColor().getIndex());
                Font dataFont = wb.createFont();
                dataFont.setFontName("Arial");
                dataFont.setFontHeightInPoints((short) 10);
                dataFont.setColor(excel.color().index);
                style.setFont(dataFont);
                styles.put(key, style);
            }
        }
        return styles;
    }

    /**
     * 创建单元格
     */
    public Cell createHeadCell(Excel attr, Row row, int column)
    {
        // 创建列
        Cell cell = row.createCell(column);
        // 写入列信息
        cell.setCellValue(this.getTitleName(attr));
        setDataValidation(attr, row, column);
        cell.setCellStyle(styles.get(StringUtils.format("header_{}_{}", attr.headerColor(), attr.headerBackgroundColor())));
        if (isSubList())
        {
            // 填充默认样式，防止合并单元格样式失效
            sheet.setDefaultColumnStyle(column, styles.get(StringUtils.format("data_{}_{}_{}", attr.align(), attr.color(), attr.backgroundColor())));
            if (attr.needMerge())
            {
                sheet.addMergedRegion(new CellRangeAddress(rownum - 1, rownum, column, column));
            }
        }
        return cell;
    }

    /**
     * 设置单元格信息
     * 
     * @param value 单元格值
     * @param attr 注解相关
     * @param cell 单元格信息
     */
    public void setCellVo(Object value, Excel attr, Cell cell)
    {
        if (ColumnType.STRING == attr.cellType())
        {
            String cellValue = Convert.toStr(value);
            // 对于任何以表达式触发字符 =-+@开头的单元格，直接使用tab字符作为前缀，防止CSV注入。
            if (StringUtils.startsWithAny(cellValue, FORMULA_STR))
            {
                cellValue = RegExUtils.replaceFirst(cellValue, FORMULA_REGEX_STR, "\t$0");
            }
            if (value instanceof Collection && StringUtils.equals("[]", cellValue))
            {
                cellValue = StringUtils.EMPTY;
            }
            cell.setCellValue(StringUtils.isNull(cellValue) ? attr.defaultValue() : cellValue + attr.suffix());
        }
        else if (ColumnType.NUMERIC == attr.cellType())
        {
            if (StringUtils.isNotNull(value))
            {
                cell.setCellValue(StringUtils.contains(Convert.toStr(value), ".") ? Convert.toDouble(value) : Convert.toInt(value));
            }
        }
        else if (ColumnType.LINK_LIST == attr.cellType())
        {
            List<String> links = (List<String>)value;
            CreationHelper createHelper = wb.getCreationHelper();
            links.forEach(link->{
                Hyperlink hyperlink = createHelper.createHyperlink(HyperlinkType.FILE);
                hyperlink.setAddress(link);
                cell.setHyperlink(hyperlink);
                cell.setCellValue(link);
            });
        }
        else if (ColumnType.IMAGE == attr.cellType())
        {
            ClientAnchor anchor = new XSSFClientAnchor(0, 0, 0, 0, (short) cell.getColumnIndex(), cell.getRow().getRowNum(), (short) (cell.getColumnIndex() + 1), cell.getRow().getRowNum() + 1);
            String imagePath = Convert.toStr(value);
            if (StringUtils.isNotEmpty(imagePath))
            {
                byte[] data = ImageUtils.getImage(imagePath);
                getDrawingPatriarch(cell.getSheet()).createPicture(anchor,
                        cell.getSheet().getWorkbook().addPicture(data, getImageType(data)));
            }
        }
        else if (ColumnType.IMAGE_LIST == attr.cellType())
        {
            String path = Convert.toStr(value);
            Drawing drawing = getDrawingPatriarch(cell.getSheet());
            if (StringUtils.isNotEmpty(path))
            {
                String[] ps = path.split(",");
                List<String> paths = Arrays.stream(ps).filter(p->StringUtils.isNotBlank(p)).collect(Collectors.toList());
                // 计算单元格的长宽
                double cellWidth = sheet.getColumnWidthInPixels(cell.getColumnIndex());
                int span = (int)(cellWidth/paths.size());

                for(int i=0;i<paths.size();i++) {
                    try {
                        String p = paths.get(i);
                        int x1=span*i*Units.EMU_PER_PIXEL;
                        int x2 = -span*(paths.size()-i-1)*Units.EMU_PER_PIXEL;
                        ClientAnchor anchor = drawing.createAnchor(x1, 0, x2, 0,  (short) cell.getColumnIndex(), cell.getRow().getRowNum(), (short) (cell.getColumnIndex() + 1), cell.getRow().getRowNum() + 1);
                        anchor.setAnchorType(ClientAnchor.AnchorType.MOVE_AND_RESIZE);
                        byte[] data = ImageUtils.getImage(p);
                        Picture picture = drawing.createPicture(anchor,
                                cell.getSheet().getWorkbook().addPicture(data, getImageType(data)));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    /**
     * 获取画布
     */
    public static Drawing<?> getDrawingPatriarch(Sheet sheet)
    {
        if (sheet.getDrawingPatriarch() == null)
        {
            sheet.createDrawingPatriarch();
        }
        return sheet.getDrawingPatriarch();
    }

    /**
     * 获取图片类型,设置图片插入类型
     */
    public int getImageType(byte[] value)
    {
        String type = FileTypeUtils.getFileExtendName(value);
        if ("JPG".equalsIgnoreCase(type))
        {
            return Workbook.PICTURE_TYPE_JPEG;
        }
        else if ("PNG".equalsIgnoreCase(type))
        {
            return Workbook.PICTURE_TYPE_PNG;
        }
        return Workbook.PICTURE_TYPE_JPEG;
    }

    /**
     * 创建表格样式
     */
    public void setDataValidation(Excel attr, Row row, int column)
    {
        if (this.getTitleName(attr).indexOf("注：") >= 0)
        {
            sheet.setColumnWidth(column, 6000);
        }
        else
        {
            // 设置列宽
            sheet.setColumnWidth(column, (int) ((attr.width() + 0.72) * 256));
        }

        String[] combo = null;
        if(attr.comboHandler()!=null && !attr.comboHandler().equals(ExcelComboHandlerAdapter.class)) {
            combo = this.dataFormatComboHandlerAdapter(attr,row.getCell(column)).toArray(new String[0]);
        } else
        {
            combo = attr.combo();
        }

        if (StringUtils.isNotEmpty(combo) || combo.length > 0) {
            if (combo.length > 15 || StringUtils.join(combo).length() > 255) {
                // 如果下拉数大于15或字符串长度大于255，则使用一个新sheet存储，避免生成的模板下拉值获取不到
                setXSSFValidationWithHidden(sheet, combo, attr.prompt(), 1, sheetSize, column, column);
            } else {
                // 提示信息或只能选择不能输入的列内容.
                setPromptOrValidation(sheet, combo, attr.prompt(), 1, sheetSize, column, column);
            }
        }
    }

    /**
     * 添加单元格
     */
    public Cell addCell(Excel attr, Row row, T vo, Field field, int column)
    {
        Cell cell = null;
        try
        {
            // 设置行高
            row.setHeight(maxHeight);
            // 根据Excel中设置情况决定是否导出,有些情况需要保持为空,希望用户填写这一列.
            if (attr.isExport())
            {
                // 创建cell
                cell = row.createCell(column);
                if (isSubListValue(vo) && getListCellValue(vo).size() > 1 && attr.needMerge())
                {
                    CellRangeAddress cellAddress = new CellRangeAddress(subMergedFirstRowNum, subMergedLastRowNum, column, column);
                    sheet.addMergedRegion(cellAddress);
                }
                cell.setCellStyle(styles.get(StringUtils.format("data_{}_{}_{}", attr.align(), attr.color(), attr.backgroundColor())));

                // 用于读取对象中的属性
                Object value = getTargetValue(vo, field, attr);
                String dateFormat = attr.dateFormat();
                String readConverterExp = attr.readConverterExp();
                String separator = attr.separator();
                String dictType = attr.dictType();
                if (StringUtils.isNotEmpty(dateFormat) && StringUtils.isNotNull(value))
                {
                    cell.setCellValue(parseDateToStr(dateFormat, value));
                }
                else if (StringUtils.isNotEmpty(readConverterExp) && StringUtils.isNotNull(value))
                {
                    cell.setCellValue(convertByExp(Convert.toStr(value), readConverterExp, separator));
                }
                else if (StringUtils.isNotEmpty(dictType) && StringUtils.isNotNull(value))
                {
                    if (!sysDictMap.containsKey(dictType + value))
                    {
                        String lable = convertDictByExp(Convert.toStr(value), dictType, separator);
                        sysDictMap.put(dictType + value, lable);
                    }
                    cell.setCellValue(sysDictMap.get(dictType + value));
                }
                else if (value instanceof BigDecimal && -1 != attr.scale())
                {
                    cell.setCellValue((((BigDecimal) value).setScale(attr.scale(), attr.roundingMode())).doubleValue());
                }
                else if (!attr.handler().equals(ExcelHandlerAdapter.class))
                {
                    cell.setCellValue(dataFormatHandlerAdapter(value, attr, cell));
                }
                else
                {
                    // 设置列类型
                    setCellVo(value, attr, cell);
                }
                addStatisticsData(column, Convert.toStr(value), attr);
            }
        }
        catch (Exception e)
        {
            log.error("导出Excel失败{}", e);
        }
        return cell;
    }

    /**
     * 设置 POI XSSFSheet 单元格提示或选择框
     * 
     * @param sheet 表单
     * @param textlist 下拉框显示的内容
     * @param promptContent 提示内容
     * @param firstRow 开始行
     * @param endRow 结束行
     * @param firstCol 开始列
     * @param endCol 结束列
     */
    public void setPromptOrValidation(Sheet sheet, String[] textlist, String promptContent, int firstRow, int endRow,
            int firstCol, int endCol)
    {
        DataValidationHelper helper = sheet.getDataValidationHelper();
        DataValidationConstraint constraint = textlist.length > 0 ? helper.createExplicitListConstraint(textlist) : helper.createCustomConstraint("DD1");
        CellRangeAddressList regions = new CellRangeAddressList(firstRow, endRow, firstCol, endCol);
        DataValidation dataValidation = helper.createValidation(constraint, regions);
        if (StringUtils.isNotEmpty(promptContent))
        {
            // 如果设置了提示信息则鼠标放上去提示
            dataValidation.createPromptBox("", promptContent);
            dataValidation.setShowPromptBox(true);
        }
        // 处理Excel兼容性问题
        if (dataValidation instanceof XSSFDataValidation)
        {
            dataValidation.setSuppressDropDownArrow(true);
            dataValidation.setShowErrorBox(true);
        }
        else
        {
            dataValidation.setSuppressDropDownArrow(false);
        }
        sheet.addValidationData(dataValidation);
    }

    /**
     * 设置某些列的值只能输入预制的数据,显示下拉框（兼容超出一定数量的下拉框）.
     * 
     * @param sheet 要设置的sheet.
     * @param textlist 下拉框显示的内容
     * @param promptContent 提示内容
     * @param firstRow 开始行
     * @param endRow 结束行
     * @param firstCol 开始列
     * @param endCol 结束列
     */
    public void setXSSFValidationWithHidden(Sheet sheet, String[] textlist, String promptContent, int firstRow, int endRow, int firstCol, int endCol)
    {
        String hideSheetName = "combo_" + firstCol + "_" + endCol;
        Sheet hideSheet = wb.createSheet(hideSheetName); // 用于存储 下拉菜单数据
        for (int i = 0; i < textlist.length; i++)
        {
            hideSheet.createRow(i).createCell(0).setCellValue(textlist[i]);
        }
        // 创建名称，可被其他单元格引用
        Name name = wb.createName();
        name.setNameName(hideSheetName + "_data");
        name.setRefersToFormula(hideSheetName + "!$A$1:$A$" + textlist.length);
        DataValidationHelper helper = sheet.getDataValidationHelper();
        // 加载下拉列表内容
        DataValidationConstraint constraint = helper.createFormulaListConstraint(hideSheetName + "_data");
        // 设置数据有效性加载在哪个单元格上,四个参数分别是：起始行、终止行、起始列、终止列
        CellRangeAddressList regions = new CellRangeAddressList(firstRow, endRow, firstCol, endCol);
        // 数据有效性对象
        DataValidation dataValidation = helper.createValidation(constraint, regions);
        if (StringUtils.isNotEmpty(promptContent))
        {
            // 如果设置了提示信息则鼠标放上去提示
            dataValidation.createPromptBox("", promptContent);
            dataValidation.setShowPromptBox(true);
        }
        // 处理Excel兼容性问题
        if (dataValidation instanceof XSSFDataValidation)
        {
            dataValidation.setSuppressDropDownArrow(true);
            dataValidation.setShowErrorBox(true);
        }
        else
        {
            dataValidation.setSuppressDropDownArrow(false);
        }

        sheet.addValidationData(dataValidation);
        // 设置hiddenSheet隐藏
        wb.setSheetHidden(wb.getSheetIndex(hideSheet), true);
    }

    /**
     * 解析导出值 0=男,1=女,2=未知
     * 
     * @param propertyValue 参数值
     * @param converterExp 翻译注解
     * @param separator 分隔符
     * @return 解析后值
     */
    public static String convertByExp(String propertyValue, String converterExp, String separator)
    {
        StringBuilder propertyString = new StringBuilder();
        String[] convertSource = converterExp.split(",");
        for (String item : convertSource)
        {
            String[] itemArray = item.split("=");
            if (StringUtils.containsAny(propertyValue, separator))
            {
                for (String value : propertyValue.split(separator))
                {
                    if (itemArray[0].equals(value))
                    {
                        propertyString.append(itemArray[1] + separator);
                        break;
                    }
                }
            }
            else
            {
                if (itemArray[0].equals(propertyValue))
                {
                    return itemArray[1];
                }
            }
        }
        return StringUtils.stripEnd(propertyString.toString(), separator);
    }

    /**
     * 反向解析值 男=0,女=1,未知=2
     * 
     * @param propertyValue 参数值
     * @param converterExp 翻译注解
     * @param separator 分隔符
     * @return 解析后值
     */
    public static String reverseByExp(String propertyValue, String converterExp, String separator)
    {
        StringBuilder propertyString = new StringBuilder();
        String[] convertSource = converterExp.split(",");
        for (String item : convertSource)
        {
            String[] itemArray = item.split("=");
            if (StringUtils.containsAny(propertyValue, separator))
            {
                for (String value : propertyValue.split(separator))
                {
                    if (itemArray[1].equals(value))
                    {
                        propertyString.append(itemArray[0] + separator);
                        break;
                    }
                }
            }
            else
            {
                if (itemArray[1].equals(propertyValue))
                {
                    return itemArray[0];
                }
            }
        }
        return StringUtils.stripEnd(propertyString.toString(), separator);
    }

    /**
     * 解析字典值
     * 
     * @param dictValue 字典值
     * @param dictType 字典类型
     * @param separator 分隔符
     * @return 字典标签
     */
    public static String convertDictByExp(String dictValue, String dictType, String separator)
    {
        return DictUtils.getDictLabel(dictType, dictValue, separator);
    }

    /**
     * 反向解析值字典值
     * 
     * @param dictLabel 字典标签
     * @param dictType 字典类型
     * @param separator 分隔符
     * @return 字典值
     */
    public static String reverseDictByExp(String dictLabel, String dictType, String separator)
    {
        return DictUtils.getDictValue(dictType, dictLabel, separator);
    }

    /**
     * 数据处理器
     *
     * @param excel 数据注解
     * @return
     */
    public List<String> dataFormatComboHandlerAdapter(Excel excel, Cell cell)
    {
        try
        {
            Object instance = excel.comboHandler().newInstance();
            Method formatMethod = excel.comboHandler().getMethod("format", new Class[] { Map.class, Cell.class, Workbook.class });
            return (List<String>)formatMethod.invoke(instance,  this.params, cell, this.wb);
        }
        catch (Exception e)
        {
            log.error("不能格式化数据 " + excel.comboHandler(), e);
        }
        return new ArrayList<>();
    }

    /**
     * 数据处理器
     * 
     * @param value 数据值
     * @param excel 数据注解
     * @return
     */
    public String dataFormatHandlerAdapter(Object value, Excel excel, Cell cell)
    {
        try
        {
            Object instance = excel.handler().newInstance();
            Method formatMethod = excel.handler().getMethod("format", new Class[] { Object.class, String[].class, Cell.class, Workbook.class,Map.class });
            value = formatMethod.invoke(instance, value, excel.args(), cell, this.wb, this.params);
        }
        catch (Exception e)
        {
            log.error("不能格式化数据 " + excel.handler(), e.getMessage());
        }
        return Convert.toStr(value);
    }

    /**
     * 合计统计信息
     */
    private void addStatisticsData(Integer index, String text, Excel entity)
    {
        if (entity != null && entity.isStatistics())
        {
            Double temp = 0D;
            if (!statistics.containsKey(index))
            {
                statistics.put(index, temp);
            }
            try
            {
                temp = Double.valueOf(text);
            }
            catch (NumberFormatException e)
            {
            }
            statistics.put(index, statistics.get(index) + temp);
        }
    }

    /**
     * 创建统计行
     */
    public void addStatisticsRow()
    {
        if (statistics.size() > 0)
        {
            Row row = sheet.createRow(sheet.getLastRowNum() + 1);
            Set<Integer> keys = statistics.keySet();
            Cell cell = row.createCell(0);
            cell.setCellStyle(styles.get("total"));
            cell.setCellValue("合计");

            for (Integer key : keys)
            {
                cell = row.createCell(key);
                cell.setCellStyle(styles.get("total"));
                cell.setCellValue(DOUBLE_FORMAT.format(statistics.get(key)));
            }
            statistics.clear();
        }
    }

    /**
     * 编码文件名
     */
    public String encodingFilename(String filename)
    {
        filename = UUID.randomUUID() + "_" + filename + ".xlsx";
        return filename;
    }

    /**
     * 获取下载路径
     * 
     * @param filename 文件名称
     */
    public String getAbsoluteFile(String filename)
    {
        String downloadPath = Cat2BugConfig.getDownloadPath() + filename;
        File desc = new File(downloadPath);
        if (!desc.getParentFile().exists())
        {
            desc.getParentFile().mkdirs();
        }
        return downloadPath;
    }

    /**
     * 获取bean中的属性值
     * 
     * @param vo 实体对象
     * @param field 字段
     * @param excel 注解
     * @return 最终的属性值
     * @throws Exception
     */
    private Object getTargetValue(T vo, Field field, Excel excel) throws Exception
    {
        Object o = field.get(vo);
        if (StringUtils.isNotEmpty(excel.targetAttr()))
        {
            String target = excel.targetAttr();
            if (target.contains("."))
            {
                String[] targets = target.split("[.]");
                for (String name : targets)
                {
                    o = getValue(o, name);
                }
            }
            else
            {
                o = getValue(o, target);
            }
        }
        return o;
    }

    /**
     * 以类的属性的get方法方法形式获取值
     * 
     * @param o
     * @param name
     * @return value
     * @throws Exception
     */
    private Object getValue(Object o, String name) throws Exception
    {
        if (StringUtils.isNotNull(o) && StringUtils.isNotEmpty(name))
        {
            Class<?> clazz = o.getClass();
            Field field = clazz.getDeclaredField(name);
            field.setAccessible(true);
            o = field.get(o);
        }
        return o;
    }

    /**
     * 得到所有定义字段
     */
    private void createExcelField()
    {
        this.fields = getFields();
        this.fields = this.fields.stream().sorted(Comparator.comparing(objects -> ((Excel) objects[1]).sort())).collect(Collectors.toList());
        this.maxHeight = getRowHeight();
    }

    /**
     * 获取字段注解信息
     */
    public List<Object[]> getFields()
    {
        List<Object[]> fields = new ArrayList<Object[]>();
        List<Field> tempFields = new ArrayList<>();
        tempFields.addAll(Arrays.asList(clazz.getSuperclass().getDeclaredFields()));
        tempFields.addAll(Arrays.asList(clazz.getDeclaredFields()));
        for (Field field : tempFields)
        {
            if (!ArrayUtils.contains(this.excludeFields, field.getName()))
            {
                // 单注解
                if (field.isAnnotationPresent(Excel.class))
                {
                    Excel attr = field.getAnnotation(Excel.class);
                    if (attr != null && (attr.type() == Type.ALL || attr.type() == type))
                    {
                        field.setAccessible(true);
                        fields.add(new Object[] { field, attr });
                    }
                    if (Collection.class.isAssignableFrom(field.getType()))
                    {
                        subMethod = getSubMethod(field.getName(), clazz);
                        ParameterizedType pt = (ParameterizedType) field.getGenericType();
                        Class<?> subClass = (Class<?>) pt.getActualTypeArguments()[0];
                        this.subFields = FieldUtils.getFieldsListWithAnnotation(subClass, Excel.class);
                    }
                }

                // 多注解
                if (field.isAnnotationPresent(Excels.class))
                {
                    Excels attrs = field.getAnnotation(Excels.class);
                    Excel[] excels = attrs.value();
                    for (Excel attr : excels)
                    {
                        if (!ArrayUtils.contains(this.excludeFields, field.getName() + "." + attr.targetAttr())
                                && (attr != null && (attr.type() == Type.ALL || attr.type() == type)))
                        {
                            field.setAccessible(true);
                            fields.add(new Object[] { field, attr });
                        }
                    }
                }
            }
        }
        return fields;
    }

    /**
     * 根据注解获取最大行高
     */
    public short getRowHeight()
    {
        double maxHeight = 0;
        for (Object[] os : this.fields)
        {
            Excel excel = (Excel) os[1];
            maxHeight = Math.max(maxHeight, excel.height());
        }
        return (short) (maxHeight * 20);
    }

    /**
     * 创建一个工作簿
     */
    public void createWorkbook()
    {
        this.wb = new SXSSFWorkbook(500);
        this.sheet = wb.createSheet();
        wb.setSheetName(0, sheetName);
        this.styles = createStyles(wb);
    }

    /**
     * 创建工作表
     * 
     * @param sheetNo sheet数量
     * @param index 序号
     */
    public void createSheet(int sheetNo, int index)
    {
        // 设置工作表的名称.
        if (sheetNo > 1 && index > 0)
        {
            this.sheet = wb.createSheet();
            this.createTitle();
            wb.setSheetName(index, sheetName + index);

            if(this.excelListener != null) {
                Integer curRowNum = this.excelListener.sheetCreated(sheet, rownum);
                if(curRowNum!=null) {
                    rownum = curRowNum + 1;
                }
            }
        }
    }

    /**
     * 获取单元格值
     * 
     * @param row 获取的行
     * @param column 获取单元格列号
     * @return 单元格值
     */
    public Object getCellValue(Row row, int column)
    {
        if (row == null)
        {
            return row;
        }
        Object val = "";
        try
        {
            Cell cell = row.getCell(column);
            if (StringUtils.isNotNull(cell))
            {
                if (cell.getCellType() == CellType.NUMERIC || cell.getCellType() == CellType.FORMULA)
                {
                    val = cell.getNumericCellValue();
                    if (DateUtil.isCellDateFormatted(cell))
                    {
                        val = DateUtil.getJavaDate((Double) val); // POI Excel 日期格式转换
                    }
                    else
                    {
                        if ((Double) val % 1 != 0)
                        {
                            val = new BigDecimal(val.toString());
                        }
                        else
                        {
                            val = new DecimalFormat("0").format(val);
                        }
                    }
                }
                else if (cell.getCellType() == CellType.STRING)
                {
                    val = cell.getStringCellValue();
                }
                else if (cell.getCellType() == CellType.BOOLEAN)
                {
                    val = cell.getBooleanCellValue();
                }
                else if (cell.getCellType() == CellType.ERROR)
                {
                    val = cell.getErrorCellValue();
                }

            }
        }
        catch (Exception e)
        {
            return val;
        }
        return val;
    }

    /**
     * 判断是否是空行
     * 
     * @param row 判断的行
     * @return
     */
    private boolean isRowEmpty(Row row)
    {
        if (row == null)
        {
            return true;
        }
        for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++)
        {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK)
            {
                return false;
            }
        }
        return true;
    }

    /**
     * 获取Excel2003图片
     *
     * @param sheet 当前sheet对象
     * @param workbook 工作簿对象
     * @return Map key:图片单元格索引（1_1）String，value:图片流PictureData
     */
    public static Map<String, List<PictureData>> getSheetPictures03(HSSFSheet sheet, HSSFWorkbook workbook)
    {
        Map<String, List<PictureData>> sheetIndexPicMap = new HashMap<>();
        List<HSSFPictureData> pictures = workbook.getAllPictures();
        if (!pictures.isEmpty())
        {
            for (HSSFShape shape : sheet.getDrawingPatriarch().getChildren())
            {
                HSSFClientAnchor anchor = (HSSFClientAnchor) shape.getAnchor();
                if (shape instanceof HSSFPicture)
                {
                    HSSFPicture pic = (HSSFPicture) shape;
                    int pictureIndex = pic.getPictureIndex() - 1;
                    HSSFPictureData picData = pictures.get(pictureIndex);
                    String picIndex = anchor.getRow1() + "_" + anchor.getCol1();
                    if(sheetIndexPicMap.containsKey(picIndex)==false) {
                        sheetIndexPicMap.put(picIndex, new ArrayList<>());
                    }
                    sheetIndexPicMap.get(picIndex).add(picData);
                }
            }
            return sheetIndexPicMap;
        }
        else
        {
            return sheetIndexPicMap;
        }
    }

    /**
     * 获取Excel2007图片
     *
     * @param sheet 当前sheet对象
     * @param workbook 工作簿对象
     * @return Map key:图片单元格索引（1_1）String，value:图片流PictureData
     */
    public static Map<String, List<PictureData>> getSheetPictures07(XSSFSheet sheet, XSSFWorkbook workbook)
    {
        Map<String, List<PictureData>> sheetIndexPicMap = new HashMap<>();
        for (POIXMLDocumentPart dr : sheet.getRelations())
        {
            if (dr instanceof XSSFDrawing)
            {
                XSSFDrawing drawing = (XSSFDrawing) dr;
                List<XSSFShape> shapes = drawing.getShapes();
                for (XSSFShape shape : shapes)
                {
                    if (shape instanceof XSSFPicture)
                    {
                        XSSFPicture pic = (XSSFPicture) shape;
                        XSSFClientAnchor anchor = pic.getPreferredSize();
                        org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.CTMarker ctMarker = anchor.getFrom();
                        String picIndex = ctMarker.getRow() + "_" + ctMarker.getCol();
                        if(sheetIndexPicMap.containsKey(picIndex)==false) {
                            sheetIndexPicMap.put(picIndex, new ArrayList<PictureData>());
                        }
                        sheetIndexPicMap.get(picIndex).add(pic.getPictureData());
                    }
                }
            }
        }

        return sheetIndexPicMap;
    }

    /**
     * 格式化不同类型的日期对象
     * 
     * @param dateFormat 日期格式
     * @param val 被格式化的日期对象
     * @return 格式化后的日期字符
     */
    public String parseDateToStr(String dateFormat, Object val)
    {
        if (val == null)
        {
            return "";
        }
        String str;
        if (val instanceof Date)
        {
            str = DateUtils.parseDateToStr(dateFormat, (Date) val);
        }
        else if (val instanceof LocalDateTime)
        {
            str = DateUtils.parseDateToStr(dateFormat, DateUtils.toDate((LocalDateTime) val));
        }
        else if (val instanceof LocalDate)
        {
            str = DateUtils.parseDateToStr(dateFormat, DateUtils.toDate((LocalDate) val));
        }
        else
        {
            str = val.toString();
        }
        return str;
    }

    /**
     * 是否有对象的子列表
     */
    public boolean isSubList()
    {
        return StringUtils.isNotNull(subFields) && subFields.size() > 0;
    }

    /**
     * 是否有对象的子列表，集合不为空
     */
    public boolean isSubListValue(T vo)
    {
        return StringUtils.isNotNull(subFields) && subFields.size() > 0 && StringUtils.isNotNull(getListCellValue(vo)) && getListCellValue(vo).size() > 0;
    }

    /**
     * 获取集合的值
     */
    public Collection<?> getListCellValue(Object obj)
    {
        Object value;
        try
        {
            value = subMethod.invoke(obj, new Object[] {});
        }
        catch (Exception e)
        {
            return new ArrayList<Object>();
        }
        return (Collection<?>) value;
    }

    /**
     * 获取对象的子列表方法
     * 
     * @param name 名称
     * @param pojoClass 类对象
     * @return 子列表方法
     */
    public Method getSubMethod(String name, Class<?> pojoClass)
    {
        StringBuffer getMethodName = new StringBuffer("get");
        getMethodName.append(name.substring(0, 1).toUpperCase());
        getMethodName.append(name.substring(1));
        Method method = null;
        try
        {
            method = pojoClass.getMethod(getMethodName.toString(), new Class[] {});
        }
        catch (Exception e)
        {
            log.error("获取对象异常{}", e.getMessage());
        }
        return method;
    }

    /**
     * 写Excel中的嵌入图片到静态资源路径下
     * @param picturePath
     * @param cell
     * @return
     * @throws IOException
     * @throws InvalidFormatException
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    private String writeEmbedImage(Map<String, PackagePart> picturePath, Cell cell) throws IOException, InvalidFormatException, ParserConfigurationException, SAXException {
        // 根据正则查找Excel中的图片是否和当前单元格中的图片相匹配，匹配了则写入图片到静态资源路径，并返回路径
        String patternStr = "^\\=DISPIMG\\(\\\"(ID_[A-Z0-9]+)\\\",[0-9]+\\)";
        Pattern pattern = Pattern.compile(patternStr);
        Matcher matcher = pattern.matcher(cell.getStringCellValue());
        while (matcher.find()) {
            // 获取单元格中匹配的图片ID
            String imgId = matcher.group(1);
            if(StringUtils.isBlank(imgId)) continue;
            if(picturePath.containsKey(imgId)) {
                // 找到匹配的图片
                PackagePart part = picturePath.get(imgId);
                // 写图片到静态资源路径
                IFileService fileService = SpringUtils.getBean(IFileService.class);
                return fileService.uploadPackagePart(part, true);
            }
        }
        return null;
    }

    /**
     * 获取excel压缩包中rid和图片ID的映射关系
     * @param part 包
     * @return rid与图片ID映射的map
     */
    private static Map<String, Set<String>> getImgMap(PackagePart part) throws IOException, ParserConfigurationException, SAXException {
        Map<String, Set<String>> mapImg = new HashMap<>();
        PackagePartName partName = part.getPartName();
        String name = partName.getName();
        // 获取根节点
        if ("/xl/cellimages.xml".equals(name)) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(part.getInputStream());

            Element root = doc.getDocumentElement();
            NodeList nodeList = root.getChildNodes();
            if(nodeList==null) return mapImg;

            for(int i=0;i<nodeList.getLength();i++) {
                Node imgNode = nodeList.item(i);
                Node xdrPic = imgNode.getFirstChild();
                Node xdrNvPicPr = xdrPic.getFirstChild();
                Node xdrBlipFill = xdrPic.getChildNodes().item(1);
                Node aBlip = xdrBlipFill.getFirstChild();
                String imgId = xdrNvPicPr.getFirstChild().getAttributes().getNamedItem("name").getNodeValue();
                String id = aBlip.getAttributes().item(0).getNodeValue();
                if (mapImg.containsKey(id)) {
                    mapImg.get(id).add(imgId);
                } else {
                    Set<String> set = new HashSet<>();
                    set.add(imgId);
                    mapImg.put(id, set);
                }
            }
        }
        return mapImg;
    }

    /**
     * 获取excel压缩包中rid和图片路径的映射关系
     * @param part 包
     * @return  rid与图片路径映射的map
     * @throws IOException
     */
    private static Map<String, String> getImgPathMap(PackagePart part) throws IOException, ParserConfigurationException, SAXException {
        Map<String, String> mapImgPath = new HashMap<>();
        PackagePartName partName = part.getPartName();
        String name = partName.getName();
        if ("/xl/_rels/cellimages.xml.rels".equals(name)) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(part.getInputStream());

            Element root = doc.getDocumentElement();
            NodeList nodeList = root.getChildNodes();
            if(nodeList==null) return mapImgPath;

            for(int i=0;i<nodeList.getLength();i++) {
                Node node = nodeList.item(i);
                if(node.getNodeType()!=Node.ELEMENT_NODE) continue;
                String id = node.getAttributes().getNamedItem("Id").getNodeValue();
                String target = node.getAttributes().getNamedItem("Target").getNodeValue();
                mapImgPath.put(id, target);
            }
        }
        return mapImgPath;
    }

    /**
     * 获取Sheet标段对应的Rid关系
     * @param part
     * @return
     */
    private static Map<Integer, List<String>> getImgOfSheetMap(PackagePart part) throws ParserConfigurationException, SAXException, IOException {
        Map<Integer, List<String>> dataMap = new HashMap<>();
        PackagePartName partName = part.getPartName();
        String name = partName.getName();
        if (name.contains("/xl/worksheets/sheet")) {
//                SAXBuilder builder = new SAXBuilder();
            // 获取文档
            String sheetNoStr = name.replace("/xl/worksheets/sheet", "").replace(".xml", "");
            Integer sheetNo = Integer.valueOf(sheetNoStr) - 1;
            // 步骤1：创建SAXParserFactory实例
            SAXParserFactory factory = SAXParserFactory.newInstance();
            // 步骤2：创建SAXParser实例
            SAXParser parser = factory.newSAXParser();
            SAXParserHandler handler = new SAXParserHandler();
            parser.parse(part.getInputStream(), handler);
            List<String> rows = handler.getRows();
            dataMap.put(sheetNo, rows);
        }
        return dataMap;
    }

    /**
     * 获取嵌入图片Map
     * @param inputStream
     * @return
     * @throws IOException
     * @throws InvalidFormatException
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    private static Map<String, PackagePart> getEmbedPictureMap(InputStream inputStream) throws IOException, InvalidFormatException, ParserConfigurationException, SAXException {
        OPCPackage opc = null;
        try {
            //包管理工具打开压缩包
            opc = PackageHelper.open(inputStream);
            //获取所有包文件
            List<PackagePart> parts = opc.getParts();
            //获取每个工作表中的包文件
            return getEmbedPictures(parts);
        } finally {
            if(opc!=null) {
                opc.close();
            }
        }
    }

    /**
     * 获取Sheet中图片对象列表的Map
     * @param parts 所有文件
     * @return  Map
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    private static Map<String, PackagePart> getEmbedPictures(List<PackagePart> parts) throws IOException, ParserConfigurationException, SAXException {
        Map<String, Set<String>> mapImg = new HashMap<>();
        Map<String, String> mapImgPath = new HashMap<>();

        for (PackagePart part : parts) {
            // RID与图片ID映射的MAP
            mapImg.putAll(getImgMap(part));
            // RID与图片路径映射的MAP
            mapImgPath.putAll(getImgPathMap(part));
        }
        // 获取图片ID与图片路径的Map
        Map<String, Set<String>> imgMap = new HashMap<>();
        for (String id : mapImg.keySet()) {
            Set<String> imgIds = mapImg.get(id);
            String path = mapImgPath.get(id);
            imgMap.put(path, imgIds);
        }

        // 转为图片id为key，PackagePart为value的Map对象
        Map<String, PackagePart> ret = new HashMap<>();
        parts.stream().forEach(p->{
            String path = p.getPartName().getName().substring(4);
            if(imgMap.containsKey(path)) {
                Set<String> ids = imgMap.get(path);
                ids.stream().forEach(id->{
                    ret.put(id, p);
                });
            }
        });
        return ret;
    }

}
