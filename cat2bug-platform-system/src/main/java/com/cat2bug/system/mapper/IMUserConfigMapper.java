package com.cat2bug.system.mapper;

import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import com.cat2bug.system.mybatis.QuarkusMapperDataSources;
import com.cat2bug.common.domain.im.IMUserConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface IMUserConfigMapper {

    IMUserConfig selectImUserConfigByProjectAndMember(@Param("projectId") Long projectId,
                                                      @Param("memberId") Long memberId);

    List<IMUserConfig> selectImUserConfigList(IMUserConfig imUserConfig);

    int insertImUserConfig(IMUserConfig imUserConfig);

    int updateImUserConfig(IMUserConfig imUserConfig);

    int deleteImUserConfigByImConfigId(Long imConfigId);
}
