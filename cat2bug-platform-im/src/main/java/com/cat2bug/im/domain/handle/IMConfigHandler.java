package com.cat2bug.im.domain.handle;

import com.alibaba.fastjson2.JSON;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.im.domain.IMConfig;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-07-18 00:40
 * @Version: 1.0.0
 */
public class IMConfigHandler  extends BaseTypeHandler<IMConfig> {
    @Override
    public void setNonNullParameter(PreparedStatement preparedStatement, int i, IMConfig imConfig, JdbcType jdbcType) throws SQLException {
        preparedStatement.setString(i,JSON.toJSONString(imConfig));
    }

    @Override
    public IMConfig getNullableResult(ResultSet resultSet, String s) throws SQLException {
        return this.toIMConfig(resultSet.getString(s));
    }

    @Override
    public IMConfig getNullableResult(ResultSet resultSet, int i) throws SQLException {
        return this.toIMConfig(resultSet.getString(i));
    }

    @Override
    public IMConfig getNullableResult(CallableStatement callableStatement, int i) throws SQLException {
        return this.toIMConfig(callableStatement.getString(i));
    }

    private IMConfig toIMConfig(String json) {
        if(StringUtils.isNotBlank(json)){
            if(json.charAt(0)=='"') {
                json = json.substring(1);
            }
            if(json.charAt(json.length()-1)=='"') {
                json = json.substring(0,json.length()-1);
            }
            json = json.replace("\\\"","\"");
            return JSON.parseObject(json,IMConfig.class);
        }
        return null;
    }
}
