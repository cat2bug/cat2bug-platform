package com.cat2bug.quarkus.system.mapper;

import org.apache.ibatis.annotations.Mapper;
import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import com.cat2bug.quarkus.system.mybatis.QuarkusMapperDataSources;

import com.cat2bug.quarkus.common.domain.SysGitWebhookDelivery;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface SysGitWebhookDeliveryMapper
{
    int insert(SysGitWebhookDelivery row);

    List<SysGitWebhookDelivery> selectRecentByWebhookId(
            @Param("webhookId") Long webhookId,
            @Param("limit") int limit);

    int deleteOlderThanKeepCount(
            @Param("webhookId") Long webhookId,
            @Param("keepCount") int keepCount);
}
