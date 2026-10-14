package com.cat2bug.system.mybatis.open;

import com.cat2bug.common.domain.open.ApiMemberBaseInfo;
import com.cat2bug.common.util.StringUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ApiMemberBaseInfoTypeHandler extends BaseTypeHandler<ApiMemberBaseInfo> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, ApiMemberBaseInfo parameter, JdbcType jdbcType)
            throws SQLException {
        try {
            ps.setString(i, MAPPER.writeValueAsString(parameter));
        } catch (Exception e) {
            throw new SQLException("Failed to serialize ApiMemberBaseInfo", e);
        }
    }

    @Override
    public ApiMemberBaseInfo getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parse(rs.getString(columnName));
    }

    @Override
    public ApiMemberBaseInfo getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parse(rs.getString(columnIndex));
    }

    @Override
    public ApiMemberBaseInfo getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parse(cs.getString(columnIndex));
    }

    private ApiMemberBaseInfo parse(String json) throws SQLException {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        try {
            return MAPPER.readValue(json, ApiMemberBaseInfo.class);
        } catch (Exception e) {
            throw new SQLException("Failed to parse ApiMemberBaseInfo JSON", e);
        }
    }
}
