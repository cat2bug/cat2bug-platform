package com.cat2bug.common.utils.poi;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-11-09 00:11
 * @Version: 1.0.0
 */
class SAXParserHandler  extends DefaultHandler {
    String value = null;

    List<String> rows = new ArrayList<>();

    int rowIndex = 0;

    public List<String> getRows() {
        return rows;
    }

    /**
     * 用来标识解析开始
     */
    @Override
    public void startDocument() throws SAXException {
        // TODO Auto-generated method stub
        super.startDocument();
        // System.out.println("SAX解析开始");
    }

    /**
     * 用来标识解析结束
     */
    @Override
    public void endDocument() throws SAXException {
        // TODO Auto-generated method stub
        super.endDocument();
        // System.out.println("SAX解析结束");
    }

    /**
     * 解析xml元素
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        // 调用DefaultHandler类的startElement方法
        super.startElement(uri, localName, qName, attributes);
        if (qName.equals("row")) {
            value = "";
        }

    }

    @Override
    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        //调用DefaultHandler类的endElement方法
        super.endElement(uri, localName, qName);
        if (qName.equals("row")) {
            if (value != null && value.contains("DISPIMG")) {
                value = value.substring(value.lastIndexOf("DISPIMG(")).replace("DISPIMG(\"", "");
                value = value.substring(0, value.indexOf("\""));
                rows.add(rowIndex, value);
            } else {
                rows.add(rowIndex, null);
            }
            rowIndex++;
            value = "";
        }
    }

    @Override
    public void characters(char[] ch, int start, int length)
            throws SAXException {
        super.characters(ch, start, length);
        value += new String(ch, start, length);
    }
}
