package com.cat2bug.system.mapper;

import com.cat2bug.system.domain.SysGitWebhookDelivery;
import org.apache.ibatis.annotations.Param;

import java.util.List;

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
