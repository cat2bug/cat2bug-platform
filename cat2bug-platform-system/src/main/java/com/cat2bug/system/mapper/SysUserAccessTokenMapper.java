package com.cat2bug.system.mapper;

import com.cat2bug.common.domain.SysUserAccessToken;
import com.cat2bug.system.mybatis.QuarkusMapperDataSources;
import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface SysUserAccessTokenMapper
{
    int insert(SysUserAccessToken row);

    SysUserAccessToken selectByHash(@Param("tokenHash") String tokenHash);

    SysUserAccessToken selectById(@Param("tokenId") Long tokenId);

    /** 个人中心列表：手动令牌与每条 OAuth 授权当前的刷新令牌，不含已吊销 */
    List<SysUserAccessToken> selectVisibleByUserId(@Param("userId") Long userId);

    int revokeById(@Param("tokenId") Long tokenId, @Param("revokedAt") Date revokedAt);

    int revokeGrant(@Param("grantId") String grantId, @Param("revokedAt") Date revokedAt);

    int revokeAllByUserId(@Param("userId") Long userId, @Param("revokedAt") Date revokedAt);

    int touch(@Param("tokenId") Long tokenId, @Param("usedAt") Date usedAt, @Param("ip") String ip);

    /** OAuth 访问令牌被使用时，同步到该授权当前的刷新令牌，供个人中心展示 */
    int touchGrantRefresh(@Param("grantId") String grantId, @Param("usedAt") Date usedAt, @Param("ip") String ip);

    int deleteInactiveBefore(@Param("before") Date before);
}
