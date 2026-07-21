package com.cat2bug.system.mapper;

import com.cat2bug.system.domain.SysGitReleaseAsset;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Git 发行版附件 Mapper
 */
public interface SysGitReleaseAssetMapper
{
    SysGitReleaseAsset selectSysGitReleaseAssetByAssetId(Long assetId);

    List<SysGitReleaseAsset> selectSysGitReleaseAssetListByReleaseId(Long releaseId);

    int countSysGitReleaseAssetByReleaseId(Long releaseId);

    int insertSysGitReleaseAsset(SysGitReleaseAsset asset);

    int deleteSysGitReleaseAssetByAssetId(Long assetId);

    int deleteSysGitReleaseAssetByReleaseId(Long releaseId);
}
