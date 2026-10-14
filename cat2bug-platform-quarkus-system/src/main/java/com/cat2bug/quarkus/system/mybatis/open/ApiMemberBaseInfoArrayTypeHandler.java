package com.cat2bug.quarkus.system.mybatis.open;

import com.cat2bug.quarkus.common.domain.open.ApiMemberBaseInfo;
import com.cat2bug.quarkus.common.util.StringUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ApiMemberBaseInfoArrayTypeHandler extends BaseTypeHandler<List<ApiMemberBaseInfo>> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<ApiMemberBaseInfo>> TYPE = new TypeReference<>() {};

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<ApiMemberBaseInfo> parameter, JdbcType jdbcType)
            throws SQLException {
        try {
            ps.setString(i, MAPPER.writeValueAsString(parameter));
        } catch (Exception e) {
            throw new SQLException("Failed to serialize ApiMemberBaseInfo list", e);
        }
    }

    @Override
    public List<ApiMemberBaseInfo> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parse(rs.getString(columnName));
    }

    @Override
    public List<ApiMemberBaseInfo> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parse(rs.getString(columnIndex));
    }

    @Override
    public List<ApiMemberBaseInfo> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parse(cs.getString(columnIndex));
    }

    private List<ApiMemberBaseInfo> parse(String json) throws SQLException {
        if (StringUtils.isEmpty(json)) {
            return new ArrayList<>();
        }
        try {
            return MAPPER.readValue(json, TYPE);
        } catch (Exception e) {
            throw new SQLException("Failed to parse ApiMemberBaseInfo list JSON", e);
        }
    }
}
