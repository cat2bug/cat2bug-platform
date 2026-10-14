package com.cat2bug.common.excel;

import com.cat2bug.common.exception.ApiException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * 对 FastExcel 生成的 xlsx 做 OOXML 级后处理，补上 FastExcel 没有 API 的能力：
 * 原生图表（drawing + chart 部件）与工作表默认列宽（{@code baseColWidth}）。
 * <p>
 * 依赖 FastExcel 的包布局：工作表固定为 {@code xl/worksheets/sheet{N}.xml}（N 从 1 开始，按创建顺序），
 * 根节点已声明 {@code xmlns:r}。
 */
public final class XlsxPatcher {

    private static final String CONTENT_TYPES = "[Content_Types].xml";
    private static final String REL_NS = "http://schemas.openxmlformats.org/package/2006/relationships";
    private static final String REL_DRAWING =
            "http://schemas.openxmlformats.org/officeDocument/2006/relationships/drawing";
    private static final String REL_CHART =
            "http://schemas.openxmlformats.org/officeDocument/2006/relationships/chart";
    private static final String CT_DRAWING = "application/vnd.openxmlformats-officedocument.drawing+xml";
    private static final String CT_CHART = "application/vnd.openxmlformats-officedocument.drawingml.chart+xml";
    private static final String DRAWING_REL_ID = "rIdCat2BugDrawing";
    /** Excel 线宽单位 EMU：1 磅 = 12700。 */
    private static final int LINE_WIDTH_EMU = 2 * 12700;

    private final Map<Integer, List<ExcelChart>> charts = new TreeMap<>();
    private final Map<Integer, Double> baseColumnWidths = new TreeMap<>();

    public static XlsxPatcher of() {
        return new XlsxPatcher();
    }

    /** @param sheetNumber 工作表序号，从 1 开始 */
    public XlsxPatcher chart(int sheetNumber, ExcelChart chart) {
        charts.computeIfAbsent(sheetNumber, k -> new ArrayList<>()).add(chart);
        return this;
    }

    /** 等价 POI {@code setDefaultColumnWidth}，作用于未单独设宽的所有列。 */
    public XlsxPatcher baseColumnWidth(int sheetNumber, double widthChars) {
        baseColumnWidths.put(sheetNumber, widthChars);
        return this;
    }

    public byte[] apply(byte[] xlsx, String errorMessage) {
        if (charts.isEmpty() && baseColumnWidths.isEmpty()) {
            return xlsx;
        }
        try {
            Map<String, byte[]> parts = readZip(xlsx);
            String contentTypes = text(parts, CONTENT_TYPES);
            int chartNo = 0;
            for (Map.Entry<Integer, List<ExcelChart>> entry : charts.entrySet()) {
                List<ExcelChart> sheetCharts = entry.getValue().stream()
                        .filter(c -> !c.series().isEmpty()).toList();
                if (sheetCharts.isEmpty()) {
                    continue;
                }
                int sheetNo = entry.getKey();
                String sheetPart = "xl/worksheets/sheet" + sheetNo + ".xml";
                String sheetXml = text(parts, sheetPart);
                String drawingPart = "xl/drawings/drawing" + sheetNo + ".xml";

                StringBuilder anchors = new StringBuilder();
                StringBuilder drawingRels = new StringBuilder();
                for (int i = 0; i < sheetCharts.size(); i++) {
                    chartNo++;
                    String chartPart = "xl/charts/chart" + chartNo + ".xml";
                    String relId = "rId" + (i + 1);
                    parts.put(chartPart, utf8(chartXml(sheetCharts.get(i))));
                    anchors.append(anchorXml(sheetCharts.get(i), relId, i + 2));
                    drawingRels.append(relationship(relId, REL_CHART, "../charts/chart" + chartNo + ".xml"));
                    contentTypes = addOverride(contentTypes, "/" + chartPart, CT_CHART);
                }
                parts.put(drawingPart, utf8(xmlDecl()
                        + "<xdr:wsDr xmlns:xdr=\"http://schemas.openxmlformats.org/drawingml/2006/spreadsheetDrawing\""
                        + " xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\">"
                        + anchors + "</xdr:wsDr>"));
                parts.put("xl/drawings/_rels/drawing" + sheetNo + ".xml.rels",
                        utf8(relationships(drawingRels.toString())));
                contentTypes = addOverride(contentTypes, "/" + drawingPart, CT_DRAWING);

                String sheetRelsPart = "xl/worksheets/_rels/sheet" + sheetNo + ".xml.rels";
                String drawingRel = relationship(DRAWING_REL_ID, REL_DRAWING, "../drawings/drawing" + sheetNo + ".xml");
                String sheetRels = parts.containsKey(sheetRelsPart)
                        ? text(parts, sheetRelsPart).replace("</Relationships>", drawingRel + "</Relationships>")
                        : relationships(drawingRel);
                parts.put(sheetRelsPart, utf8(sheetRels));
                parts.put(sheetPart, utf8(insertDrawing(sheetXml)));
            }
            for (Map.Entry<Integer, Double> entry : baseColumnWidths.entrySet()) {
                String sheetPart = "xl/worksheets/sheet" + entry.getKey() + ".xml";
                String sheetXml = text(parts, sheetPart).replace("<sheetFormatPr ",
                        "<sheetFormatPr baseColWidth=\"" + formatNumber(entry.getValue()) + "\" ");
                parts.put(sheetPart, utf8(sheetXml));
            }
            parts.put(CONTENT_TYPES, utf8(contentTypes));
            return writeZip(parts);
        } catch (IOException | IllegalStateException e) {
            throw new ApiException(errorMessage);
        }
    }

    /** CT_Worksheet 中 drawing 位于 headerFooter 等之后、legacyDrawing/tableParts/extLst 之前。 */
    private static String insertDrawing(String sheetXml) {
        String element = "<drawing r:id=\"" + DRAWING_REL_ID + "\"/>";
        for (String marker : new String[] {"<legacyDrawing", "<legacyDrawingHF", "<picture", "<oleObjects",
                "<controls", "<webPublishItems", "<tableParts", "<extLst", "</worksheet>"}) {
            int idx = sheetXml.indexOf(marker);
            if (idx >= 0) {
                return sheetXml.substring(0, idx) + element + sheetXml.substring(idx);
            }
        }
        throw new IllegalStateException("worksheet 结构异常");
    }

    private static String anchorXml(ExcelChart chart, String relId, int shapeId) {
        return "<xdr:twoCellAnchor editAs=\"oneCell\">"
                + "<xdr:from><xdr:col>" + chart.col1() + "</xdr:col><xdr:colOff>0</xdr:colOff>"
                + "<xdr:row>" + chart.row1() + "</xdr:row><xdr:rowOff>0</xdr:rowOff></xdr:from>"
                + "<xdr:to><xdr:col>" + chart.col2() + "</xdr:col><xdr:colOff>0</xdr:colOff>"
                + "<xdr:row>" + chart.row2() + "</xdr:row><xdr:rowOff>0</xdr:rowOff></xdr:to>"
                + "<xdr:graphicFrame macro=\"\"><xdr:nvGraphicFramePr>"
                + "<xdr:cNvPr id=\"" + shapeId + "\" name=\"Chart " + (shapeId - 1) + "\"/>"
                + "<xdr:cNvGraphicFramePr/></xdr:nvGraphicFramePr>"
                + "<xdr:xfrm><a:off x=\"0\" y=\"0\"/><a:ext cx=\"0\" cy=\"0\"/></xdr:xfrm>"
                + "<a:graphic><a:graphicData uri=\"http://schemas.openxmlformats.org/drawingml/2006/chart\">"
                + "<c:chart xmlns:c=\"http://schemas.openxmlformats.org/drawingml/2006/chart\""
                + " xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\""
                + " r:id=\"" + relId + "\"/>"
                + "</a:graphicData></a:graphic></xdr:graphicFrame><xdr:clientData/></xdr:twoCellAnchor>";
    }

    static String chartXml(ExcelChart chart) {
        boolean line = chart.type() == ExcelChart.Type.LINE;
        StringBuilder xml = new StringBuilder(xmlDecl());
        xml.append("<c:chartSpace xmlns:c=\"http://schemas.openxmlformats.org/drawingml/2006/chart\"")
                .append(" xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\"")
                .append(" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">")
                .append("<c:roundedCorners val=\"0\"/><c:chart>")
                .append(title(chart.title()))
                .append("<c:autoTitleDeleted val=\"0\"/><c:plotArea><c:layout/>");
        if (line) {
            xml.append("<c:lineChart><c:grouping val=\"standard\"/><c:varyColors val=\"0\"/>");
        } else {
            xml.append("<c:barChart><c:barDir val=\"col\"/><c:grouping val=\"clustered\"/><c:varyColors val=\"0\"/>");
        }
        for (int i = 0; i < chart.series().size(); i++) {
            xml.append(seriesXml(chart.series().get(i), i, line));
        }
        if (line) {
            xml.append("<c:marker val=\"1\"/>");
        } else {
            xml.append("<c:gapWidth val=\"150\"/>");
        }
        xml.append("<c:axId val=\"1\"/><c:axId val=\"2\"/>").append(line ? "</c:lineChart>" : "</c:barChart>");
        xml.append("<c:catAx><c:axId val=\"1\"/><c:scaling><c:orientation val=\"minMax\"/></c:scaling>")
                .append("<c:delete val=\"0\"/><c:axPos val=\"b\"/><c:numFmt formatCode=\"General\" sourceLinked=\"1\"/>")
                .append("<c:majorTickMark val=\"out\"/><c:minorTickMark val=\"none\"/><c:tickLblPos val=\"nextTo\"/>")
                .append("<c:crossAx val=\"2\"/><c:crosses val=\"autoZero\"/><c:auto val=\"1\"/>")
                .append("<c:lblAlgn val=\"ctr\"/><c:lblOffset val=\"100\"/><c:noMultiLvlLbl val=\"0\"/></c:catAx>");
        xml.append("<c:valAx><c:axId val=\"2\"/><c:scaling><c:orientation val=\"minMax\"/></c:scaling>")
                .append("<c:delete val=\"0\"/><c:axPos val=\"l\"/><c:majorGridlines/>");
        if (chart.valueAxisTitle() != null) {
            xml.append(title(chart.valueAxisTitle()));
        }
        xml.append("<c:numFmt formatCode=\"General\" sourceLinked=\"1\"/>")
                .append("<c:majorTickMark val=\"out\"/><c:minorTickMark val=\"none\"/><c:tickLblPos val=\"nextTo\"/>")
                .append("<c:crossAx val=\"1\"/><c:crosses val=\"autoZero\"/><c:crossBetween val=\"between\"/></c:valAx>")
                .append("</c:plotArea>");
        if (chart.legend() != null && chart.legend() != ExcelChart.Legend.NONE) {
            xml.append("<c:legend><c:legendPos val=\"")
                    .append(chart.legend() == ExcelChart.Legend.TOP ? "t" : "r")
                    .append("\"/><c:overlay val=\"0\"/></c:legend>");
        }
        xml.append("<c:plotVisOnly val=\"1\"/><c:dispBlanksAs val=\"gap\"/></c:chart></c:chartSpace>");
        return xml.toString();
    }

    private static String seriesXml(ExcelChartSeries series, int index, boolean line) {
        String color = ExcelChart.ECHARTS_COLORS.get(index % ExcelChart.ECHARTS_COLORS.size());
        String fill = "<a:solidFill><a:srgbClr val=\"" + color + "\"/></a:solidFill>";
        StringBuilder xml = new StringBuilder("<c:ser>");
        xml.append("<c:idx val=\"").append(index).append("\"/><c:order val=\"").append(index).append("\"/>")
                .append("<c:tx><c:v>").append(escape(series.name())).append("</c:v></c:tx>")
                .append("<c:spPr>").append(fill).append("<a:ln w=\"").append(LINE_WIDTH_EMU).append("\">")
                .append(fill).append("</a:ln></c:spPr>");
        if (line) {
            xml.append("<c:marker><c:symbol val=\"circle\"/></c:marker>");
        } else {
            xml.append("<c:invertIfNegative val=\"0\"/>")
                    .append("<c:dLbls><c:dLblPos val=\"outEnd\"/><c:showLegendKey val=\"0\"/><c:showVal val=\"1\"/>")
                    .append("<c:showCatName val=\"0\"/><c:showSerName val=\"0\"/><c:showPercent val=\"0\"/>")
                    .append("<c:showBubbleSize val=\"0\"/></c:dLbls>");
        }
        xml.append("<c:cat>").append(categoriesXml(series)).append("</c:cat>");
        xml.append("<c:val>").append(valuesXml(series)).append("</c:val>");
        if (line) {
            xml.append("<c:smooth val=\"1\"/>");
        }
        return xml.append("</c:ser>").toString();
    }

    private static String categoriesXml(ExcelChartSeries series) {
        StringBuilder points = new StringBuilder("<c:ptCount val=\"").append(series.categories().size()).append("\"/>");
        for (int i = 0; i < series.categories().size(); i++) {
            String value = series.categories().get(i);
            points.append("<c:pt idx=\"").append(i).append("\"><c:v>")
                    .append(escape(value == null ? "" : value)).append("</c:v></c:pt>");
        }
        if (series.categoryRef() == null) {
            return "<c:strLit>" + points + "</c:strLit>";
        }
        return "<c:strRef><c:f>" + escape(series.categoryRef()) + "</c:f><c:strCache>" + points
                + "</c:strCache></c:strRef>";
    }

    private static String valuesXml(ExcelChartSeries series) {
        StringBuilder points = new StringBuilder("<c:formatCode>General</c:formatCode><c:ptCount val=\"")
                .append(series.values().size()).append("\"/>");
        for (int i = 0; i < series.values().size(); i++) {
            Number value = series.values().get(i);
            if (value != null) {
                points.append("<c:pt idx=\"").append(i).append("\"><c:v>")
                        .append(formatNumber(value.doubleValue())).append("</c:v></c:pt>");
            }
        }
        if (series.valueRef() == null) {
            return "<c:numLit>" + points + "</c:numLit>";
        }
        return "<c:numRef><c:f>" + escape(series.valueRef()) + "</c:f><c:numCache>" + points
                + "</c:numCache></c:numRef>";
    }

    private static String title(String text) {
        return "<c:title><c:tx><c:rich><a:bodyPr/><a:p><a:r><a:t>" + escape(text)
                + "</a:t></a:r></a:p></c:rich></c:tx><c:overlay val=\"0\"/></c:title>";
    }

    private static String formatNumber(double value) {
        if (value == Math.rint(value) && !Double.isInfinite(value) && Math.abs(value) < 1e15) {
            return Long.toString((long) value);
        }
        return Double.toString(value);
    }

    private static String relationships(String body) {
        return xmlDecl() + "<Relationships xmlns=\"" + REL_NS + "\">" + body + "</Relationships>";
    }

    private static String relationship(String id, String type, String target) {
        return "<Relationship Id=\"" + id + "\" Type=\"" + type + "\" Target=\"" + target + "\"/>";
    }

    private static String addOverride(String contentTypes, String partName, String contentType) {
        return contentTypes.replace("</Types>",
                "<Override PartName=\"" + partName + "\" ContentType=\"" + contentType + "\"/></Types>");
    }

    private static String xmlDecl() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>";
    }

    private static String escape(String text) {
        StringBuilder out = new StringBuilder(text.length());
        for (char ch : text.toCharArray()) {
            switch (ch) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '"' -> out.append("&quot;");
                default -> {
                    if (ch >= 0x20 || ch == '\t' || ch == '\n' || ch == '\r') {
                        out.append(ch);
                    }
                }
            }
        }
        return out.toString();
    }

    private static String text(Map<String, byte[]> parts, String name) {
        byte[] bytes = parts.get(name);
        if (bytes == null) {
            throw new IllegalStateException("缺少部件 " + name);
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static byte[] utf8(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * FastExcel（opczip）本地头里的 Zip64 大小为 0、真实大小只在数据描述符中，
     * {@code ZipInputStream} 会报 invalid entry size，必须经中央目录（{@link ZipFile}）读取。
     */
    private static Map<String, byte[]> readZip(byte[] xlsx) throws IOException {
        Path temp = Files.createTempFile("cat2bug-xlsx", ".zip");
        try {
            Files.write(temp, xlsx);
            Map<String, byte[]> parts = new LinkedHashMap<>();
            try (ZipFile zip = new ZipFile(temp.toFile())) {
                Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    try (InputStream in = zip.getInputStream(entry)) {
                        parts.put(entry.getName(), in.readAllBytes());
                    }
                }
            }
            return parts;
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private static byte[] writeZip(Map<String, byte[]> parts) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            for (Map.Entry<String, byte[]> part : parts.entrySet()) {
                zip.putNextEntry(new ZipEntry(part.getKey()));
                zip.write(part.getValue());
                zip.closeEntry();
            }
        }
        return out.toByteArray();
    }
}
