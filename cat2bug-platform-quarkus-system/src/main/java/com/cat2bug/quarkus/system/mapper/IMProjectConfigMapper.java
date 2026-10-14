package com.cat2bug.quarkus.system.mapper;

import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import com.cat2bug.quarkus.system.mybatis.QuarkusMapperDataSources;
import com.cat2bug.quarkus.common.domain.im.IMProjectConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface IMProjectConfigMapper {

    IMProjectConfig selectImProjectConfigByConfigId(Long configId);

    IMProjectConfig selectImProjectConfigByProjectIdAndSystemCode(@Param("projectId") Long projectId,
                                                                  @Param("systemCode") String systemCode);

    List<IMProjectConfig> selectImProjectConfigList(IMProjectConfig imProjectConfig);

    int insertImProjectConfig(IMProjectConfig imProjectConfig);

    int updateImProjectConfig(IMProjectConfig imProjectConfig);

    int deleteImProjectConfigByConfigId(Long configId);
}
