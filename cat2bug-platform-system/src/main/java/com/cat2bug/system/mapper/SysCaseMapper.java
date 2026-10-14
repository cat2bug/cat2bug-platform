package com.cat2bug.system.mapper;

import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import com.cat2bug.system.mybatis.QuarkusMapperDataSources;
import com.cat2bug.common.domain.SysCase;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface SysCaseMapper {

    long countSysCaseList(@Param("query") SysCase query);

    List<SysCase> selectSysCaseList(@Param("query") SysCase query,
                                    @Param("offset") int offset,
                                    @Param("limit") int limit);

    List<SysCase> selectSysCaseListAll(@Param("query") SysCase query);

    SysCase selectSysCaseByCaseId(@Param("caseId") Long caseId);

    SysCase selectSysCaseByCaseNum(@Param("projectId") Long projectId, @Param("caseNum") Long caseNum);

    long getCaseMaxNumOfProject(@Param("projectId") Long projectId);

    int insertSysCase(SysCase sysCase);

    int updateSysCase(SysCase sysCase);

    int deleteSysCaseByCaseId(@Param("caseId") Long caseId);

    int deleteSysCaseByCaseNumAndProjectId(@Param("projectId") Long projectId, @Param("caseNum") Long caseNum);

    List<Long> selectSysCaseIdList(@Param("moduleIds") Set<Long> moduleIds);

    List<Long> selectSysCaseIdListByLevel(@Param("projectId") Long projectId, @Param("level") Long level);

    SysCase selectPrevSysCase(@Param("query") SysCase query);

    SysCase selectNextSysCase(@Param("query") SysCase query);
}
