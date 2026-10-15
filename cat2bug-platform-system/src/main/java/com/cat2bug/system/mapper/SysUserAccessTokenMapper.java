package com.cat2bug.system.mapper;

import com.cat2bug.common.domain.SysUserAccessToken;
import com.cat2bug.system.mybatis.QuarkusMapperDataSources;
import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface SysUserAccessTokenMapper
{
    int insert(SysUserAccessToken row);

    SysUserAccessToken selectByHash(@Param("tokenHash") String tokenHash);

    int revokeById(@Param("tokenId") Long tokenId, @Param("revokedAt") Date revokedAt);

    int revokeGrant(@Param("grantId") String grantId, @Param("revokedAt") Date revokedAt);

    int touch(@Param("tokenId") Long tokenId, @Param("usedAt") Date usedAt, @Param("ip") String ip);

    int deleteInactiveBefore(@Param("before") Date before);
}
