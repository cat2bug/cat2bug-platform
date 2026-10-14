package com.cat2bug.system.mapper;

import com.cat2bug.common.domain.SysOAuthCode;
import com.cat2bug.system.mybatis.QuarkusMapperDataSources;
import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface SysOAuthCodeMapper
{
    int insert(SysOAuthCode row);

    SysOAuthCode selectByHash(@Param("codeHash") String codeHash);

    /** 只有未使用时才置为已使用；返回 0 表示已被用过 */
    int markUsed(@Param("codeHash") String codeHash);

    int deleteExpiredBefore(@Param("before") Date before);
}
