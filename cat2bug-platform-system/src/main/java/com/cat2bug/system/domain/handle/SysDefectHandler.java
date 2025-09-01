package com.cat2bug.system.domain.handle;

import com.alibaba.fastjson2.JSON;
import com.cat2bug.common.core.domain.entity.SysDefect;
import com.cat2bug.common.utils.StringUtils;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-04-06 15:54
 * @Version: 1.0.0
 */
public class SysDefectHandler extends BaseTypeHandler<SysDefect> {
    @Override
    public void setNonNullParameter(PreparedStatement preparedStatement, int i, SysDefect sysDefect, JdbcType jdbcType) throws SQLException {
        preparedStatement.setString(i,JSON.toJSONString(sysDefect));
    }

    @Override
    public SysDefect getNullableResult(ResultSet resultSet, String s) throws SQLException {
        return this.toSysDefect(resultSet.getString(s));
    }

    @Override
    public SysDefect getNullableResult(ResultSet resultSet, int i) throws SQLException {
        return this.toSysDefect(resultSet.getString(i));
    }

    @Override
    public SysDefect getNullableResult(CallableStatement callableStatement, int i) throws SQLException {
        return this.toSysDefect(callableStatement.getString(i));
    }

    private SysDefect toSysDefect(String json) {
        if(StringUtils.isNotBlank(json)){
            if(json.charAt(0)=='"') {
                json = json.substring(1);
            }
            if(json.charAt(json.length()-1)=='"') {
                json = json.substring(0,json.length()-1);
            }
            json = json.replace("\\\"","\"");
            return JSON.parseObject(json,SysDefect.class);
        }
        return null;
    }
}
