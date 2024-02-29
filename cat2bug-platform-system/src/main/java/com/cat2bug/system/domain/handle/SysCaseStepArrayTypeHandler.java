package com.cat2bug.system.domain.handle;

import com.alibaba.fastjson2.JSON;
import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.system.domain.SysCaseStep;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-01-28 00:26
 * @Version: 1.0.0
 */
public class SysCaseStepArrayTypeHandler extends BaseTypeHandler<List<SysCaseStep>> {
    @Override
    public void setNonNullParameter(PreparedStatement preparedStatement, int i, List<SysCaseStep> sysCaseSteps, JdbcType jdbcType) throws SQLException {
        preparedStatement.setString(i, JSON.toJSONString(sysCaseSteps));
    }

    @Override
    public List<SysCaseStep> getNullableResult(ResultSet resultSet, String s) throws SQLException {
        return this.convertToList(resultSet.getString(s));
    }

    @Override
    public List<SysCaseStep> getNullableResult(ResultSet resultSet, int i) throws SQLException {
        return this.convertToList(resultSet.getString(i));
    }

    @Override
    public List<SysCaseStep> getNullableResult(CallableStatement callableStatement, int i) throws SQLException {
        return this.convertToList(callableStatement.getString(i));
    }
    // 字符串转换为list
    private List<SysCaseStep> convertToList(String strArray) {
        if(StringUtils.isNotBlank(strArray)){
            String json = strArray.replaceAll("^\"|\"$", "").replace("\\\"","\"");
            return JSON.parseArray(json,SysCaseStep.class);
        }
        return new ArrayList<>();
    }
}
