package com.cat2bug.system.domain.handle;

import com.alibaba.fastjson2.JSON;
import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.common.utils.StringUtils;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @Author: yuzhantao
 * @CreateTime: 2023-11-28 10:04
 * @Version: 1.0.0
 */
public class SysUserTypeHandler extends BaseTypeHandler<SysUser> {
    @Override
    public void setNonNullParameter(PreparedStatement preparedStatement, int i, SysUser longs, JdbcType jdbcType) throws SQLException {
        preparedStatement.setString(i, JSON.toJSONString(longs));
    }

    @Override
    public SysUser getNullableResult(ResultSet resultSet, String s) throws SQLException {
        return this.convertToList(resultSet.getString(s));
    }

    @Override
    public SysUser getNullableResult(ResultSet resultSet, int i) throws SQLException {
        return this.convertToList(resultSet.getString(i));
    }

    @Override
    public SysUser getNullableResult(CallableStatement callableStatement, int i) throws SQLException {
        return this.convertToList(callableStatement.getString(i));
    }

    // 字符串转换为list
    private SysUser convertToList(String json) {
        if(StringUtils.isNotBlank(json)){
            return JSON.parseObject(json,SysUser.class);
        }
        return null;
    }
}
