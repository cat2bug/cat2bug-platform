package com.cat2bug.system.domain.handle;

import com.alibaba.fastjson2.JSON;
import com.cat2bug.common.utils.StringUtils;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author: yuzhantao
 * @CreateTime: 2023-11-23 22:32
 * @Version: 1.0.0
 */
public class StringArrayTypeHandler extends BaseTypeHandler<List<String>> {
    @Override
    public void setNonNullParameter(PreparedStatement preparedStatement, int i, List<String> strings, JdbcType jdbcType) throws SQLException {
        preparedStatement.setString(i, JSON.toJSONString(strings));
    }

    @Override
    public List<String> getNullableResult(ResultSet resultSet, String s) throws SQLException {
        return this.convertToList(resultSet.getString(s));
    }

    @Override
    public List<String> getNullableResult(ResultSet resultSet, int i) throws SQLException {
        return this.convertToList(resultSet.getString(i));
    }

    @Override
    public List<String> getNullableResult(CallableStatement callableStatement, int i) throws SQLException {
        return this.convertToList(callableStatement.getString(i));
    }

    // 字符串转换为list
    private List<String> convertToList(String strArray) {
        return StringUtils.isEmpty(strArray)?new ArrayList<>() :JSON.parseArray(strArray,String.class);
    }
}
