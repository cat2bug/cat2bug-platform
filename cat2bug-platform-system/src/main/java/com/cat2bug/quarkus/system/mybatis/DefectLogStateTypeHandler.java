package com.cat2bug.quarkus.system.mybatis;

import com.cat2bug.quarkus.common.constant.DefectLogState;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DefectLogStateTypeHandler extends BaseTypeHandler<DefectLogState> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, DefectLogState parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setInt(i, parameter.ordinal());
    }

    @Override
    public DefectLogState getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return toEnum(rs.getInt(columnName));
    }

    @Override
    public DefectLogState getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return toEnum(rs.getInt(columnIndex));
    }

    @Override
    public DefectLogState getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return toEnum(cs.getInt(columnIndex));
    }

    private DefectLogState toEnum(int ordinal) {
        DefectLogState[] values = DefectLogState.values();
        if (ordinal < 0 || ordinal >= values.length) {
            return null;
        }
        return values[ordinal];
    }
}
