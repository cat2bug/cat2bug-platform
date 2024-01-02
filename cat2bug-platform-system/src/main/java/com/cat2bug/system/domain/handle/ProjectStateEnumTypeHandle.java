package com.cat2bug.system.domain.handle;

import com.cat2bug.system.domain.type.SysProjectStateEnum;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-01-01 02:51
 * @Version: 1.0.0
 */
public class ProjectStateEnumTypeHandle extends BaseTypeHandler<SysProjectStateEnum> {

    @Override
    public void setNonNullParameter(PreparedStatement preparedStatement, int i, SysProjectStateEnum sysProjectStateEnum, JdbcType jdbcType) throws SQLException {
        preparedStatement.setInt(i, sysProjectStateEnum.ordinal());
    }

    @Override
    public SysProjectStateEnum getNullableResult(ResultSet resultSet, String s) throws SQLException {
        return this.convertToEnum(resultSet.getInt(s));
    }

    @Override
    public SysProjectStateEnum getNullableResult(ResultSet resultSet, int i) throws SQLException {
        return this.convertToEnum(resultSet.getInt(i));
    }

    @Override
    public SysProjectStateEnum getNullableResult(CallableStatement callableStatement, int i) throws SQLException {
        return this.convertToEnum(callableStatement.getInt(i));
    }

    private SysProjectStateEnum convertToEnum(int i) {
        return SysProjectStateEnum.values()[i];
    }

}
