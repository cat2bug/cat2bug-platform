package com.cat2bug.system.mapper;

import org.apache.ibatis.annotations.Mapper;
import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import com.cat2bug.system.mybatis.QuarkusMapperDataSources;

import com.cat2bug.common.domain.SysGitReleaseAsset;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Git 发行版附件 Mapper
 */
@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface SysGitReleaseAssetMapper
{
    SysGitReleaseAsset selectSysGitReleaseAssetByAssetId(Long assetId);

    List<SysGitReleaseAsset> selectSysGitReleaseAssetListByReleaseId(Long releaseId);

    int countSysGitReleaseAssetByReleaseId(Long releaseId);

    int insertSysGitReleaseAsset(SysGitReleaseAsset asset);

    int deleteSysGitReleaseAssetByAssetId(Long assetId);

    int deleteSysGitReleaseAssetByReleaseId(Long releaseId);
}
