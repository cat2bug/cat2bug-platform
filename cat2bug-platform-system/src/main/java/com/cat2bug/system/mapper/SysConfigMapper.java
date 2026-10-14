package com.cat2bug.system.mapper;

import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import com.cat2bug.system.mybatis.QuarkusMapperDataSources;
import com.cat2bug.common.domain.SysConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface SysConfigMapper {

    SysConfig selectConfig(@Param("configId") Long configId, @Param("configKey") String configKey);

    List<SysConfig> selectConfigList(@Param("query") SysConfig query);

    long countConfigList(@Param("query") SysConfig query);

    SysConfig checkConfigKeyUnique(@Param("configKey") String configKey);

    int insertConfig(SysConfig config);

    int updateConfig(SysConfig config);

    int deleteConfigById(@Param("configId") Long configId);
}
