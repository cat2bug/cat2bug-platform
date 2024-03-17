package com.cat2bug.common.core.domain.handle;

import com.cat2bug.common.core.domain.type.SysDefectTypeEnum;
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
public class SysDefectTypeEnumTypeHandler  extends BaseTypeHandler<SysDefectTypeEnum> {
    @Override
    public void setNonNullParameter(PreparedStatement preparedStatement, int i, SysDefectTypeEnum sysDefectTypeEnum, JdbcType jdbcType) throws SQLException {
        preparedStatement.setInt(i, sysDefectTypeEnum.ordinal());
    }

    @Override
    public SysDefectTypeEnum getNullableResult(ResultSet resultSet, String s) throws SQLException {
        return this.convertToEnum(resultSet.getInt(s));
    }

    @Override
    public SysDefectTypeEnum getNullableResult(ResultSet resultSet, int i) throws SQLException {
        return this.convertToEnum(resultSet.getInt(i));
    }

    @Override
    public SysDefectTypeEnum getNullableResult(CallableStatement callableStatement, int i) throws SQLException {
        return this.convertToEnum(callableStatement.getInt(i));
    }

    private SysDefectTypeEnum convertToEnum(int i) {
        return SysDefectTypeEnum.values()[i];
    }
}
