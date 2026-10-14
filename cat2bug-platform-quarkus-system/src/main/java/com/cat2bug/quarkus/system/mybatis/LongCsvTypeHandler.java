package com.cat2bug.quarkus.system.mybatis;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 逗号分隔的 Long 列表（如 sys_plan_item.defect_ids 的 ",12,34"），SQL 里也按逗号做 LIKE/REPLACE 拼接。
 */
public class LongCsvTypeHandler extends BaseTypeHandler<List<Long>> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<Long> parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setString(i, parameter.stream().map(String::valueOf).collect(Collectors.joining(",")));
    }

    @Override
    public List<Long> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parse(rs.getString(columnName));
    }

    @Override
    public List<Long> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parse(rs.getString(columnIndex));
    }

    @Override
    public List<Long> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parse(cs.getString(columnIndex));
    }

    static List<Long> parse(String value) {
        List<Long> result = new ArrayList<>();
        if (value == null) {
            return result;
        }
        // 兼容早期 Quarkus 以 JSON 数组写入的数据
        String normalized = value.replace("[", "").replace("]", "").replace("\"", "");
        for (String part : normalized.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                result.add(Long.valueOf(trimmed));
            }
        }
        return result;
    }
}
