package com.cat2bug.system.mybatis;

import com.cat2bug.common.domain.SysDefect;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SysDefectJsonTypeHandler extends BaseTypeHandler<SysDefect> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, SysDefect parameter, JdbcType jdbcType)
            throws SQLException {
        try {
            ps.setString(i, MAPPER.writeValueAsString(parameter));
        } catch (Exception e) {
            throw new SQLException("Failed to serialize defect tab config JSON", e);
        }
    }

    @Override
    public SysDefect getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parse(rs.getString(columnName));
    }

    @Override
    public SysDefect getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parse(rs.getString(columnIndex));
    }

    @Override
    public SysDefect getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parse(cs.getString(columnIndex));
    }

    private SysDefect parse(String json) throws SQLException {
        if (json == null || json.isBlank()) {
            return null;
        }
        String normalized = json.trim();
        if (normalized.startsWith("\"") && normalized.endsWith("\"")) {
            normalized = normalized.substring(1, normalized.length() - 1);
            normalized = normalized.replace("\\\"", "\"");
        }
        try {
            return MAPPER.readValue(normalized, SysDefect.class);
        } catch (Exception e) {
            throw new SQLException("Failed to parse defect tab config JSON", e);
        }
    }
}
