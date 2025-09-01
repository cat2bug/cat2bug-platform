package com.cat2bug.im.domain.handle;

import com.cat2bug.common.utils.StringUtils;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @Author: yuzhantao
 * @CreateTime: 2023-11-23 22:32
 * @Version: 1.0.0
 */
public class IMJSONStringConfigTypeHandler extends BaseTypeHandler<String> {
    @Override
    public void setNonNullParameter(PreparedStatement preparedStatement, int i, String str, JdbcType jdbcType) throws SQLException {
        preparedStatement.setString(i, str);
    }

    @Override
    public String getNullableResult(ResultSet resultSet, String s) throws SQLException {
        return this.convertToList(resultSet.getString(s));
    }

    @Override
    public String getNullableResult(ResultSet resultSet, int i) throws SQLException {
        return this.convertToList(resultSet.getString(i));
    }

    @Override
    public String getNullableResult(CallableStatement callableStatement, int i) throws SQLException {
        return this.convertToList(callableStatement.getString(i));
    }

    // 字符串转换为list
    private String convertToList(String jsonString) {
        return StringUtils.isNotBlank(jsonString)?jsonString.replaceAll("^\"|\"$", "").replace("\\\"","\""):jsonString;
    }
}
