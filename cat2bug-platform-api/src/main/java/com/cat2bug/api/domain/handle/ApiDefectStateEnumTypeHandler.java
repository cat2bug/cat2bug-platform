package com.cat2bug.api.domain.handle;

import com.cat2bug.api.domain.type.ApiDefectStateEnum;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @Author: yuzhantao
 * @CreateTime: 2023-11-28 14:08
 * @Version: 1.0.0
 */
public class ApiDefectStateEnumTypeHandler extends BaseTypeHandler<ApiDefectStateEnum> {
    @Override
    public void setNonNullParameter(PreparedStatement preparedStatement, int i, ApiDefectStateEnum sysDefectTypeEnum, JdbcType jdbcType) throws SQLException {
        preparedStatement.setInt(i, sysDefectTypeEnum.ordinal());
    }

    @Override
    public ApiDefectStateEnum getNullableResult(ResultSet resultSet, String s) throws SQLException {
        return this.convertToEnum(resultSet.getInt(s));
    }

    @Override
    public ApiDefectStateEnum getNullableResult(ResultSet resultSet, int i) throws SQLException {
        return this.convertToEnum(resultSet.getInt(i));
    }

    @Override
    public ApiDefectStateEnum getNullableResult(CallableStatement callableStatement, int i) throws SQLException {
        return this.convertToEnum(callableStatement.getInt(i));
    }

    private ApiDefectStateEnum convertToEnum(int i) {
        return ApiDefectStateEnum.values()[i];
    }
}
