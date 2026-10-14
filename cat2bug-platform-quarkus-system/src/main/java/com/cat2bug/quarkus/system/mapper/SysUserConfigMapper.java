package com.cat2bug.quarkus.system.mapper;

import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import com.cat2bug.quarkus.system.mybatis.QuarkusMapperDataSources;
import com.cat2bug.quarkus.common.domain.SysUserConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface SysUserConfigMapper {

    SysUserConfig selectSysUserConfigByUserId(Long userId);

    SysUserConfig selectSysUserConfigByUserConfigId(Long userConfigId);

    int insertSysUserConfig(SysUserConfig sysUserConfig);

    int updateSysUserConfig(SysUserConfig sysUserConfig);

    /** {@code userId} 为空时不限归属（管理员）。 */
    int deleteSysUserConfigByUserConfigIds(@Param("userConfigIds") Long[] userConfigIds, @Param("userId") Long userId);
}
