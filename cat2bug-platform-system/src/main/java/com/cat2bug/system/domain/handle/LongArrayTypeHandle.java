package com.cat2bug.system.domain.handle;

import com.cat2bug.common.utils.StringUtils;
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
 * @Author: yuzhantao
 * @CreateTime: 2024-10-17 15:57
 * @Version: 1.0.0
 */
public class LongArrayTypeHandle extends BaseTypeHandler<List<Long>> {
    @Override
    public void setNonNullParameter(PreparedStatement preparedStatement, int i, List<Long> longs, JdbcType jdbcType) throws SQLException {
        preparedStatement.setString(i, longs.stream().map(s->String.valueOf(s)).collect(Collectors.joining(",")));
    }

    @Override
    public List<Long> getNullableResult(ResultSet resultSet, String s) throws SQLException {
        return this.convertToList(resultSet.getString(s));
    }

    @Override
    public List<Long> getNullableResult(ResultSet resultSet, int i) throws SQLException {
        return this.convertToList(resultSet.getString(i));
    }

    @Override
    public List<Long> getNullableResult(CallableStatement callableStatement, int i) throws SQLException {
        return this.convertToList(callableStatement.getString(i));
    }

    private List<Long> convertToList(String strArray) {
        if(StringUtils.isBlank(strArray))
            return new ArrayList<>();
        else
            return Arrays.stream(strArray.split(",")).filter(s->StringUtils.isNotBlank(s)).map(s->Long.valueOf(s)).collect(Collectors.toList());
    }
}
