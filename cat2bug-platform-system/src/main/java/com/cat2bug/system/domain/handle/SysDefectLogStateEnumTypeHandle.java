package com.cat2bug.system.domain.handle;

import com.cat2bug.system.domain.type.SysDefectLogStateEnum;
import com.cat2bug.system.domain.type.SysDefectStateEnum;
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
public class SysDefectLogStateEnumTypeHandle extends BaseTypeHandler<SysDefectLogStateEnum> {
    @Override
    public void setNonNullParameter(PreparedStatement preparedStatement, int i, SysDefectLogStateEnum sysDefectTypeEnum, JdbcType jdbcType) throws SQLException {
        preparedStatement.setInt(i, sysDefectTypeEnum.ordinal());
    }

    @Override
    public SysDefectLogStateEnum getNullableResult(ResultSet resultSet, String s) throws SQLException {
        return this.convertToEnum(resultSet.getInt(s));
    }

    @Override
    public SysDefectLogStateEnum getNullableResult(ResultSet resultSet, int i) throws SQLException {
        return this.convertToEnum(resultSet.getInt(i));
    }

    @Override
    public SysDefectLogStateEnum getNullableResult(CallableStatement callableStatement, int i) throws SQLException {
        return this.convertToEnum(callableStatement.getInt(i));
    }

    private SysDefectLogStateEnum convertToEnum(int i) {
        return SysDefectLogStateEnum.values()[i];
    }

}
