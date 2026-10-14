package com.cat2bug.system.mapper;

import org.apache.ibatis.annotations.Mapper;
import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import com.cat2bug.system.mybatis.QuarkusMapperDataSources;

import org.apache.ibatis.annotations.Param;
import com.cat2bug.common.domain.SysUserGitRepository;

/**
 * 用户关注代码仓库 Mapper
 */
@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface SysUserGitRepositoryMapper
{
    SysUserGitRepository selectByUserIdAndRepositoryId(@Param("userId") Long userId, @Param("repositoryId") Long repositoryId);

    int insertSysUserGitRepository(SysUserGitRepository row);

    int updateCollectByUserIdAndRepositoryId(SysUserGitRepository row);

    int deleteByRepositoryId(@Param("repositoryId") Long repositoryId);
}
