package com.cat2bug.system.mybatis.open;

import com.cat2bug.common.domain.open.type.ApiDefectStateEnum;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ApiDefectStateEnumTypeHandler extends BaseTypeHandler<ApiDefectStateEnum> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, ApiDefectStateEnum parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setInt(i, parameter.ordinal());
    }

    @Override
    public ApiDefectStateEnum getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return toEnum(rs.getInt(columnName));
    }

    @Override
    public ApiDefectStateEnum getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return toEnum(rs.getInt(columnIndex));
    }

    @Override
    public ApiDefectStateEnum getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return toEnum(cs.getInt(columnIndex));
    }

    private ApiDefectStateEnum toEnum(int ordinal) {
        ApiDefectStateEnum[] values = ApiDefectStateEnum.values();
        if (ordinal < 0 || ordinal >= values.length) {
            return null;
        }
        return values[ordinal];
    }
}
