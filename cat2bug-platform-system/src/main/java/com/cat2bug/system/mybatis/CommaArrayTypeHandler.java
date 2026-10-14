package com.cat2bug.system.mybatis;

import com.cat2bug.common.util.StringUtils;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 逗号分隔字符串与 {@code List<String>} 互转，对齐 Spring {@code CommaArrayTypeHandler}。
 */
public class CommaArrayTypeHandler extends BaseTypeHandler<List<String>> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<String> parameter, JdbcType jdbcType)
            throws SQLException {
        if (parameter != null) {
            ps.setString(i, parameter.stream().distinct().collect(Collectors.joining(",")));
        }
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return convertToList(rs.getString(columnName));
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return convertToList(rs.getString(columnIndex));
    }

    @Override
    public List<String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return convertToList(cs.getString(columnIndex));
    }

    private List<String> convertToList(String strArray) {
        return StringUtils.isBlank(strArray) ? new ArrayList<>() : Arrays.asList(strArray.split(","));
    }
}
