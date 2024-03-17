package com.cat2bug.system.domain.handle;

import com.cat2bug.system.domain.type.SysTempFileTypeEnum;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @Author: yuzhantao
 * @CreateTime: 2023-12-08 00:46
 * @Version: 1.0.0
 */
public class SysTempFileTypeEnumHandler  extends BaseTypeHandler<SysTempFileTypeEnum> {
    @Override
    public void setNonNullParameter(PreparedStatement preparedStatement, int i, SysTempFileTypeEnum sysTempFileTypeEnum, JdbcType jdbcType) throws SQLException {
        preparedStatement.setInt(i, sysTempFileTypeEnum.ordinal());
    }

    @Override
    public SysTempFileTypeEnum getNullableResult(ResultSet resultSet, String s) throws SQLException {
        return this.convertToEnum(resultSet.getInt(s));
    }

    @Override
    public SysTempFileTypeEnum getNullableResult(ResultSet resultSet, int i) throws SQLException {
        return this.convertToEnum(resultSet.getInt(i));
    }

    @Override
    public SysTempFileTypeEnum getNullableResult(CallableStatement callableStatement, int i) throws SQLException {
        return this.convertToEnum(callableStatement.getInt(i));
    }

    private SysTempFileTypeEnum convertToEnum(int i) {
        return SysTempFileTypeEnum.values()[i];
    }
}
