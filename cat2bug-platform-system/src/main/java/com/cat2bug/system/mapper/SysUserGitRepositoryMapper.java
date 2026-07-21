package com.cat2bug.system.mapper;

import org.apache.ibatis.annotations.Param;
import com.cat2bug.system.domain.SysUserGitRepository;

/**
 * 用户关注代码仓库 Mapper
 */
public interface SysUserGitRepositoryMapper
{
    SysUserGitRepository selectByUserIdAndRepositoryId(@Param("userId") Long userId, @Param("repositoryId") Long repositoryId);

    int insertSysUserGitRepository(SysUserGitRepository row);

    int updateCollectByUserIdAndRepositoryId(SysUserGitRepository row);

    int deleteByRepositoryId(@Param("repositoryId") Long repositoryId);
}
