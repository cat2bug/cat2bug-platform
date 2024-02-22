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
 * @CreateTime: 2023-11-28 00:21
 * @Version: 1.0.0
 */
public class LongArrayTypeHandler extends BaseTypeHandler<List<Long>> {
    @Override
    public void setNonNullParameter(PreparedStatement preparedStatement, int i, List<Long> longs, JdbcType jdbcType) throws SQLException {
        String json =  JSON.toJSONString(longs);
        preparedStatement.setString(i, json);
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

    // 字符串转换为list
    private List<Long> convertToList(String strArray) {
        if(StringUtils.isNotBlank(strArray)) {
            return JSON.parseArray(strArray.replaceAll("^\"|\"$", ""),Long.class);
        }
        return new ArrayList<>();
    }
}
