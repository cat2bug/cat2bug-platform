package com.cat2bug.framework.datasource;

import com.alibaba.druid.pool.DruidDataSource;
import com.cat2bug.common.enums.DataSourceType;

import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * 动态数据源
 * 
 * @author ruoyi
 */
public class DynamicDataSource extends AbstractRoutingDataSource
{
    public DynamicDataSource(DataSource defaultTargetDataSource, Map<Object, Object> targetDataSources)
    {
        super.setDefaultTargetDataSource(defaultTargetDataSource);
        super.setTargetDataSources(targetDataSources);
        super.afterPropertiesSet();
    }

    @Override
    protected Object determineCurrentLookupKey()
    {
        return DynamicDataSourceContextHolder.getDataSourceType();
    }

    /**
     * 安装向导提交后热切换主库（关闭被替换的 Druid 连接池）。
     */
    public synchronized void replaceMaster(DataSource newMaster)
    {
        Map<Object, DataSource> resolved = getResolvedDataSources();
        DataSource previous = resolved.get(DataSourceType.MASTER.name());
        Map<Object, Object> targets = new HashMap<>(resolved.size());
        for (Map.Entry<Object, DataSource> entry : resolved.entrySet())
        {
            Object key = entry.getKey();
            if (DataSourceType.MASTER.name().equals(String.valueOf(key)))
            {
                targets.put(key, newMaster);
            }
            else
            {
                targets.put(key, entry.getValue());
            }
        }
        if (!targets.containsKey(DataSourceType.MASTER.name()))
        {
            targets.put(DataSourceType.MASTER.name(), newMaster);
        }
        setDefaultTargetDataSource(newMaster);
        setTargetDataSources(targets);
        afterPropertiesSet();
        closeIfDruid(previous, newMaster);
    }

    private static void closeIfDruid(DataSource previous, DataSource newMaster)
    {
        if (previous != null && previous != newMaster && previous instanceof DruidDataSource druid)
        {
            druid.close();
        }
    }
}