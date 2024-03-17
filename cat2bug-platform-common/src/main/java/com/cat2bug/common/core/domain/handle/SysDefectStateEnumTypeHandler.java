package com.cat2bug.common.core.domain.handle;

import com.cat2bug.common.core.domain.type.SysDefectStateEnum;
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
public class SysDefectStateEnumTypeHandler extends BaseTypeHandler<SysDefectStateEnum> {
    @Override
    public void setNonNullParameter(PreparedStatement preparedStatement, int i, SysDefectStateEnum sysDefectTypeEnum, JdbcType jdbcType) throws SQLException {
        preparedStatement.setInt(i, sysDefectTypeEnum.ordinal());
    }

    @Override
    public SysDefectStateEnum getNullableResult(ResultSet resultSet, String s) throws SQLException {
        return this.convertToEnum(resultSet.getInt(s));
    }

    @Override
    public SysDefectStateEnum getNullableResult(ResultSet resultSet, int i) throws SQLException {
        return this.convertToEnum(resultSet.getInt(i));
    }

    @Override
    public SysDefectStateEnum getNullableResult(CallableStatement callableStatement, int i) throws SQLException {
        return this.convertToEnum(callableStatement.getInt(i));
    }

    private SysDefectStateEnum convertToEnum(int i) {
        return SysDefectStateEnum.values()[i];
    }
}
