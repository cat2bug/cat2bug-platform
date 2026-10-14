package com.cat2bug.system.mybatis;

import com.cat2bug.common.domain.im.IMConfig;
import com.cat2bug.common.util.StringUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class IMConfigTypeHandler extends BaseTypeHandler<IMConfig> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, IMConfig parameter, JdbcType jdbcType)
            throws SQLException {
        try {
            ps.setString(i, MAPPER.writeValueAsString(parameter));
        } catch (Exception e) {
            throw new SQLException("Failed to serialize IMConfig", e);
        }
    }

    @Override
    public IMConfig getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parse(rs.getString(columnName));
    }

    @Override
    public IMConfig getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parse(rs.getString(columnIndex));
    }

    @Override
    public IMConfig getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parse(cs.getString(columnIndex));
    }

    private IMConfig parse(String json) throws SQLException {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        String s = json.trim();
        if (s.startsWith("\"") && s.endsWith("\"")) {
            s = s.substring(1, s.length() - 1).replace("\\\"", "\"");
        }
        try {
            return MAPPER.readValue(s, IMConfig.class);
        } catch (Exception e) {
            throw new SQLException("Failed to parse IMConfig", e);
        }
    }
}
