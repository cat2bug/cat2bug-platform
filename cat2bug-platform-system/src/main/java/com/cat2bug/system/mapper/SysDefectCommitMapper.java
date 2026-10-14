package com.cat2bug.system.mapper;

import com.cat2bug.common.domain.SysDefectCommit;
import com.cat2bug.system.mybatis.QuarkusMapperDataSources;
import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface SysDefectCommitMapper
{
    int insert(SysDefectCommit row);

    List<SysDefectCommit> selectByDefectLogIds(@Param("defectLogIds") List<Long> defectLogIds);

    List<SysDefectCommit> selectByDefectId(@Param("defectId") Long defectId);
}
