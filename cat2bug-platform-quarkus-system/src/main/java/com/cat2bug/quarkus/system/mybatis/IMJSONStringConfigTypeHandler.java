package com.cat2bug.quarkus.system.mybatis;

import com.cat2bug.quarkus.common.util.StringUtils;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class IMJSONStringConfigTypeHandler extends BaseTypeHandler<String> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setString(i, parameter);
    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return normalize(rs.getString(columnName));
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return normalize(rs.getString(columnIndex));
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return normalize(cs.getString(columnIndex));
    }

    private String normalize(String jsonString) {
        if (StringUtils.isBlank(jsonString)) {
            return jsonString;
        }
        return jsonString.replaceAll("^\"|\"$", "").replace("\\\"", "\"");
    }
}
