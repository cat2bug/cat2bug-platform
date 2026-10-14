package com.cat2bug.quarkus.system.mybatis.open;

import com.cat2bug.quarkus.common.domain.open.type.ApiDefectTypeEnum;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ApiDefectTypeEnumTypeHandler extends BaseTypeHandler<ApiDefectTypeEnum> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, ApiDefectTypeEnum parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setInt(i, parameter.ordinal());
    }

    @Override
    public ApiDefectTypeEnum getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return toEnum(rs.getInt(columnName));
    }

    @Override
    public ApiDefectTypeEnum getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return toEnum(rs.getInt(columnIndex));
    }

    @Override
    public ApiDefectTypeEnum getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return toEnum(cs.getInt(columnIndex));
    }

    private ApiDefectTypeEnum toEnum(int ordinal) {
        ApiDefectTypeEnum[] values = ApiDefectTypeEnum.values();
        if (ordinal < 0 || ordinal >= values.length) {
            return null;
        }
        return values[ordinal];
    }
}
