package com.cat2bug.quarkus.system.mybatis;

import com.cat2bug.quarkus.common.domain.SysCaseStep;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class CaseStepListJsonTypeHandler extends BaseTypeHandler<List<SysCaseStep>> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<SysCaseStep>> TYPE = new TypeReference<>() {};

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<SysCaseStep> parameter, JdbcType jdbcType)
            throws SQLException {
        try {
            ps.setString(i, MAPPER.writeValueAsString(parameter));
        } catch (Exception e) {
            throw new SQLException("Failed to serialize case_step JSON", e);
        }
    }

    @Override
    public List<SysCaseStep> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parse(rs.getString(columnName));
    }

    @Override
    public List<SysCaseStep> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parse(rs.getString(columnIndex));
    }

    @Override
    public List<SysCaseStep> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parse(cs.getString(columnIndex));
    }

    private List<SysCaseStep> parse(String json) throws SQLException {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(json, TYPE);
        } catch (Exception e) {
            throw new SQLException("Failed to parse case_step JSON", e);
        }
    }
}
