package com.cat2bug.api.domain.handle;

import com.alibaba.fastjson.JSON;
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
public class ObjectTypeHandler extends BaseTypeHandler<Object> {
    @Override
    public void setNonNullParameter(PreparedStatement preparedStatement, int i, Object obj, JdbcType jdbcType) throws SQLException {
        preparedStatement.setString(i, JSON.toJSONString(obj));
    }

    @Override
    public Object getNullableResult(ResultSet resultSet, String s) throws SQLException {
        return JSON.parse(s);
    }


    @Override
    public String getNullableResult(ResultSet resultSet, int i) throws SQLException {
        return this.obj2String(resultSet.getString(i));
    }

    @Override
    public String getNullableResult(CallableStatement callableStatement, int i) throws SQLException {
        return this.obj2String(callableStatement.getString(i));
    }

    // 字符串转换为list
    private String obj2String(Object obj) {
        if(obj==null) return null;
        return JSON.toJSONString(obj);
    }
}
