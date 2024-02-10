package com.cat2bug.api.domain.handle;

import com.cat2bug.api.domain.type.ApiDefectTypeEnum;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @Author: yuzhantao
 * @CreateTime: 2023-11-28 13:18
 * @Version: 1.0.0
 */
public class ApiDefectTypeEnumTypeHandler extends BaseTypeHandler<ApiDefectTypeEnum> {
    @Override
    public void setNonNullParameter(PreparedStatement preparedStatement, int i, ApiDefectTypeEnum sysDefectTypeEnum, JdbcType jdbcType) throws SQLException {
        preparedStatement.setInt(i, sysDefectTypeEnum.ordinal());
    }

    @Override
    public ApiDefectTypeEnum getNullableResult(ResultSet resultSet, String s) throws SQLException {
        return this.convertToEnum(resultSet.getInt(s));
    }

    @Override
    public ApiDefectTypeEnum getNullableResult(ResultSet resultSet, int i) throws SQLException {
        return this.convertToEnum(resultSet.getInt(i));
    }

    @Override
    public ApiDefectTypeEnum getNullableResult(CallableStatement callableStatement, int i) throws SQLException {
        return this.convertToEnum(callableStatement.getInt(i));
    }

    private ApiDefectTypeEnum convertToEnum(int i) {
        return ApiDefectTypeEnum.values()[i];
    }
}
