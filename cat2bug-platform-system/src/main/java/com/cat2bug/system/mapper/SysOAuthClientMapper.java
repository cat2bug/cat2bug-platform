package com.cat2bug.system.mapper;

import com.cat2bug.common.domain.SysOAuthClient;
import com.cat2bug.system.mybatis.QuarkusMapperDataSources;
import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface SysOAuthClientMapper
{
    int insert(SysOAuthClient row);

    SysOAuthClient selectById(@Param("clientId") String clientId);

    int touch(@Param("clientId") String clientId, @Param("usedAt") Date usedAt);

    /** 删除长期未使用且没有有效令牌的客户端 */
    int deleteUnusedBefore(@Param("before") Date before, @Param("now") Date now);
}
