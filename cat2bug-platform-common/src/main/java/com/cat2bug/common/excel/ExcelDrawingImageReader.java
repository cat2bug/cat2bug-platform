package com.cat2bug.common.excel;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * 从 xlsx 读取工作表悬浮图（drawing + media），按单元格坐标（0-based {@code row_col}）归组。
 * <p>
 * 不依赖 POI；覆盖 Excel / WPS 粘贴到单元格区域的常见 {@code twoCellAnchor}/{@code oneCellAnchor}。
 */
public final class ExcelDrawingImageReader {

    private ExcelDrawingImageReader() {
    }

    /**
     * @return key = {@code row_col}（均为 0-based），value = 该单元格内图片字节（可多张）
     */
    public static Map<String, List<EmbeddedImage>> readByCell(byte[] xlsxBytes) {
        if (xlsxBytes == null || xlsxBytes.length == 0) {
            return Map.of();
        }
        Map<String, byte[]> zipEntries = readZipEntries(xlsxBytes);
        if (zipEntries.isEmpty()) {
            return Map.of();
        }
        String sheetPath = firstWorksheetPath(zipEntries);
        if (sheetPath == null) {
            return Map.of();
        }
        String sheetRelsPath = relsPathFor(sheetPath);
        Map<String, String> sheetRels = parseRelationships(zipEntries.get(sheetRelsPath), parentDir(sheetPath));
        String drawingPath = null;
        for (String target : sheetRels.values()) {
            if (target != null && target.contains("drawings/")) {
                drawingPath = normalizeZipPath(target);
                break;
            }
        }
        if (drawingPath == null || !zipEntries.containsKey(drawingPath)) {
            return Map.of();
        }
        String drawingRelsPath = relsPathFor(drawingPath);
        Map<String, String> drawingRels = parseRelationships(zipEntries.get(drawingRelsPath), parentDir(drawingPath));
        return parseDrawingAnchors(zipEntries.get(drawingPath), drawingRels, zipEntries);
    }

    public record EmbeddedImage(byte[] data, String fileName) {
    }

    private static Map<String, List<EmbeddedImage>> parseDrawingAnchors(byte[] drawingXml,
                                                                        Map<String, String> drawingRels,
                                                                        Map<String, byte[]> zipEntries) {
        Map<String, List<EmbeddedImage>> result = new LinkedHashMap<>();
        if (drawingXml == null || drawingXml.length == 0) {
            return result;
        }
        try {
            Document doc = newSecureDocumentBuilderFactory()
                    .newDocumentBuilder()
                    .parse(new ByteArrayInputStream(drawingXml));
            NodeList anchors = doc.getElementsByTagNameNS("*", "twoCellAnchor");
            collectAnchors(anchors, drawingRels, zipEntries, result);
            NodeList oneCell = doc.getElementsByTagNameNS("*", "oneCellAnchor");
            collectAnchors(oneCell, drawingRels, zipEntries, result);
        } catch (Exception ignored) {
            return result;
        }
        return result;
    }

    private static void collectAnchors(NodeList anchors, Map<String, String> drawingRels,
                                       Map<String, byte[]> zipEntries,
                                       Map<String, List<EmbeddedImage>> result) {
        if (anchors == null) {
            return;
        }
        for (int i = 0; i < anchors.getLength(); i++) {
            Node anchor = anchors.item(i);
            if (!(anchor instanceof Element element)) {
                continue;
            }
            Element from = firstChildElement(element, "from");
            if (from == null) {
                continue;
            }
            Integer row = childInt(from, "row");
            Integer col = childInt(from, "col");
            if (row == null || col == null) {
                continue;
            }
            Element blip = firstDescendantElement(element, "blip");
            if (blip == null) {
                continue;
            }
            String embedId = attrIgnoreNs(blip, "embed");
            if (embedId == null || embedId.isBlank()) {
                continue;
            }
            String mediaPath = drawingRels.get(embedId);
            if (mediaPath == null) {
                continue;
            }
            byte[] data = zipEntries.get(normalizeZipPath(mediaPath));
            if (data == null || data.length == 0) {
                continue;
            }
            String fileName = fileNameOf(mediaPath);
            result.computeIfAbsent(row + "_" + col, key -> new ArrayList<>())
                    .add(new EmbeddedImage(data, fileName));
        }
    }

    private static Map<String, byte[]> readZipEntries(byte[] xlsxBytes) {
        Map<String, byte[]> entries = new HashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(xlsxBytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                String name = normalizeZipPath(entry.getName());
                entries.put(name, zip.readAllBytes());
            }
        } catch (IOException ignored) {
            return Map.of();
        }
        return entries;
    }

    private static String firstWorksheetPath(Map<String, byte[]> zipEntries) {
        byte[] workbookRels = zipEntries.get("xl/_rels/workbook.xml.rels");
        Map<String, String> rels = parseRelationships(workbookRels, "xl");
        // workbook.xml 中 sheets 顺序即可见表顺序；勿按 rels 文件顺序（隐藏 _lists 可能排在前面）
        String rid = firstSheetRelationshipId(zipEntries.get("xl/workbook.xml"));
        if (rid != null && rels.containsKey(rid)) {
            return normalizeZipPath(rels.get(rid));
        }
        if (zipEntries.containsKey("xl/worksheets/sheet1.xml")) {
            return "xl/worksheets/sheet1.xml";
        }
        for (String target : rels.values()) {
            if (target != null && target.contains("worksheets/")) {
                return normalizeZipPath(target);
            }
        }
        return null;
    }

    private static String firstSheetRelationshipId(byte[] workbookXml) {
        if (workbookXml == null || workbookXml.length == 0) {
            return null;
        }
        try {
            Document doc = newSecureDocumentBuilderFactory()
                    .newDocumentBuilder()
                    .parse(new ByteArrayInputStream(workbookXml));
            NodeList sheets = doc.getElementsByTagNameNS("*", "sheet");
            for (int i = 0; i < sheets.getLength(); i++) {
                Node node = sheets.item(i);
                if (!(node instanceof Element el)) {
                    continue;
                }
                String state = el.getAttribute("state");
                if ("hidden".equalsIgnoreCase(state) || "veryHidden".equalsIgnoreCase(state)) {
                    continue;
                }
                String id = attrIgnoreNs(el, "id");
                if (id != null && !id.isBlank()) {
                    return id;
                }
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    private static Map<String, String> parseRelationships(byte[] relsXml, String baseDir) {
        Map<String, String> map = new LinkedHashMap<>();
        if (relsXml == null || relsXml.length == 0) {
            return map;
        }
        try {
            Document doc = newSecureDocumentBuilderFactory()
                    .newDocumentBuilder()
                    .parse(new ByteArrayInputStream(relsXml));
            NodeList nodes = doc.getElementsByTagNameNS("*", "Relationship");
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if (!(node instanceof Element el)) {
                    continue;
                }
                String id = el.getAttribute("Id");
                String target = el.getAttribute("Target");
                if (id == null || id.isBlank() || target == null || target.isBlank()) {
                    continue;
                }
                map.put(id, resolveAgainst(baseDir, target));
            }
        } catch (Exception ignored) {
            return map;
        }
        return map;
    }

    private static String resolveAgainst(String baseDir, String target) {
        String t = target.replace('\\', '/');
        if (t.startsWith("/")) {
            return normalizeZipPath(t.substring(1));
        }
        if (t.startsWith("../")) {
            String base = baseDir == null ? "" : baseDir;
            while (t.startsWith("../")) {
                t = t.substring(3);
                int slash = base.lastIndexOf('/');
                base = slash >= 0 ? base.substring(0, slash) : "";
            }
            return normalizeZipPath(base.isEmpty() ? t : base + "/" + t);
        }
        if (baseDir == null || baseDir.isBlank()) {
            return normalizeZipPath(t);
        }
        return normalizeZipPath(baseDir + "/" + t);
    }

    private static String parentDir(String path) {
        int slash = path.lastIndexOf('/');
        return slash >= 0 ? path.substring(0, slash) : "";
    }

    private static String relsPathFor(String partPath) {
        int slash = partPath.lastIndexOf('/');
        String dir = slash >= 0 ? partPath.substring(0, slash) : "";
        String name = slash >= 0 ? partPath.substring(slash + 1) : partPath;
        return normalizeZipPath(dir + "/_rels/" + name + ".rels");
    }

    private static String normalizeZipPath(String path) {
        String p = path.replace('\\', '/');
        while (p.startsWith("./")) {
            p = p.substring(2);
        }
        while (p.startsWith("/")) {
            p = p.substring(1);
        }
        return p;
    }

    private static String fileNameOf(String path) {
        String p = path.replace('\\', '/');
        int slash = p.lastIndexOf('/');
        String name = slash >= 0 ? p.substring(slash + 1) : p;
        if (name.isBlank()) {
            return "image.png";
        }
        return name.toLowerCase(Locale.ROOT);
    }

    private static Element firstChildElement(Element parent, String localName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (n instanceof Element el && localName.equals(el.getLocalName())) {
                return el;
            }
        }
        return null;
    }

    private static Element firstDescendantElement(Element parent, String localName) {
        NodeList nodes = parent.getElementsByTagNameNS("*", localName);
        if (nodes.getLength() == 0) {
            return null;
        }
        Node n = nodes.item(0);
        return n instanceof Element el ? el : null;
    }

    private static Integer childInt(Element parent, String localName) {
        Element child = firstChildElement(parent, localName);
        if (child == null || child.getTextContent() == null) {
            return null;
        }
        try {
            return Integer.parseInt(child.getTextContent().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String attrIgnoreNs(Element el, String localName) {
        if (el.hasAttribute(localName)) {
            return el.getAttribute(localName);
        }
        if (el.hasAttributeNS("http://schemas.openxmlformats.org/officeDocument/2006/relationships", localName)) {
            return el.getAttributeNS(
                    "http://schemas.openxmlformats.org/officeDocument/2006/relationships", localName);
        }
        // 部分写出器用 r:embed
        return el.getAttribute("r:" + localName);
    }

    private static DocumentBuilderFactory newSecureDocumentBuilderFactory() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setExpandEntityReferences(false);
        return factory;
    }
}
