package com.cat2bug.system.mapper;

import com.cat2bug.system.domain.SysGitRelease;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Git 发行版 Mapper
 */
public interface SysGitReleaseMapper
{
    SysGitRelease selectSysGitReleaseByReleaseId(Long releaseId);

    SysGitRelease selectSysGitReleaseByRepositoryIdAndTagName(
            @Param("repositoryId") Long repositoryId,
            @Param("tagName") String tagName);

    List<SysGitRelease> selectSysGitReleaseListByRepositoryId(
            @Param("repositoryId") Long repositoryId,
            @Param("excludeDraft") boolean excludeDraft);

    int insertSysGitRelease(SysGitRelease release);

    int updateSysGitRelease(SysGitRelease release);

    int deleteSysGitReleaseByReleaseId(Long releaseId);
}
