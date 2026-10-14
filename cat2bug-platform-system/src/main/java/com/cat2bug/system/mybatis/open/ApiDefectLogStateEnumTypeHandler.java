package com.cat2bug.system.mybatis.open;

import com.cat2bug.common.domain.open.type.ApiDefectLogStateEnum;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ApiDefectLogStateEnumTypeHandler extends BaseTypeHandler<ApiDefectLogStateEnum> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, ApiDefectLogStateEnum parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setInt(i, parameter.ordinal());
    }

    @Override
    public ApiDefectLogStateEnum getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return toEnum(rs.getInt(columnName));
    }

    @Override
    public ApiDefectLogStateEnum getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return toEnum(rs.getInt(columnIndex));
    }

    @Override
    public ApiDefectLogStateEnum getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return toEnum(cs.getInt(columnIndex));
    }

    private ApiDefectLogStateEnum toEnum(int ordinal) {
        ApiDefectLogStateEnum[] values = ApiDefectLogStateEnum.values();
        if (ordinal < 0 || ordinal >= values.length) {
            throw new IllegalArgumentException(
                    String.format("Invalid defect_log_type value: %d. Valid range: 0-%d", ordinal, values.length - 1));
        }
        return values[ordinal];
    }
}
