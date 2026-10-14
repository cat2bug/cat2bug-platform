package com.cat2bug.common.report;

import com.cat2bug.common.domain.SysDefect;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * 缺陷报告编解码器，对齐 Spring {@code com.cat2bug.common.report.DefectReportCoder}。
 */
public class DefectReportCoder implements IReportCoder<List<SysDefect>> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<SysDefect>> TYPE = new TypeReference<>() {};

    @Override
    public String encode(List<SysDefect> sysDefects) {
        try {
            return MAPPER.writeValueAsString(sysDefects);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encode defect report", e);
        }
    }

    @Override
    public List<SysDefect> decode(String code) {
        try {
            return MAPPER.readValue(code, TYPE);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decode defect report", e);
        }
    }
}
