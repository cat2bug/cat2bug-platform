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
import java.util.ArrayList;
import java.util.List;

/**
 * @Author: yuzhantao
 * @CreateTime: 2023-11-28 10:04
 * @Version: 1.0.0
 */
public class SysUserArrayTypeHandler extends BaseTypeHandler<List<SysUser>> {
    @Override
    public void setNonNullParameter(PreparedStatement preparedStatement, int i, List<SysUser> longs, JdbcType jdbcType) throws SQLException {
        preparedStatement.setString(i, JSON.toJSONString(longs));
    }

    @Override
    public List<SysUser> getNullableResult(ResultSet resultSet, String s) throws SQLException {
        return this.convertToList(resultSet.getString(s));
    }

    @Override
    public List<SysUser> getNullableResult(ResultSet resultSet, int i) throws SQLException {
        return this.convertToList(resultSet.getString(i));
    }

    @Override
    public List<SysUser> getNullableResult(CallableStatement callableStatement, int i) throws SQLException {
        return this.convertToList(callableStatement.getString(i));
    }

    // 字符串转换为list
    private List<SysUser> convertToList(String strArray) {
        if(StringUtils.isNotBlank(strArray)){
            String json = strArray.replaceAll("^\"|\"$", "");
            return JSON.parseArray(json,SysUser.class);
        }
        return new ArrayList<>();
    }
}
