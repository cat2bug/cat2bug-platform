package com.cat2bug.api.domain.handle;

import com.cat2bug.api.domain.type.ApiDefectLogStateEnum;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @Author: yuzhantao
 * @CreateTime: 2023-11-30 16:07
 * @Version: 1.0.0
 */
public class ApiDefectLogStateEnumTypeHandle extends BaseTypeHandler<ApiDefectLogStateEnum> {
    @Override
    public void setNonNullParameter(PreparedStatement preparedStatement, int i, ApiDefectLogStateEnum sysDefectTypeEnum, JdbcType jdbcType) throws SQLException {
        preparedStatement.setInt(i, sysDefectTypeEnum.ordinal());
    }

    @Override
    public ApiDefectLogStateEnum getNullableResult(ResultSet resultSet, String s) throws SQLException {
        return this.convertToEnum(resultSet.getInt(s));
    }

    @Override
    public ApiDefectLogStateEnum getNullableResult(ResultSet resultSet, int i) throws SQLException {
        return this.convertToEnum(resultSet.getInt(i));
    }

    @Override
    public ApiDefectLogStateEnum getNullableResult(CallableStatement callableStatement, int i) throws SQLException {
        return this.convertToEnum(callableStatement.getInt(i));
    }

    private ApiDefectLogStateEnum convertToEnum(int i) {
        return ApiDefectLogStateEnum.values()[i];
    }

}
