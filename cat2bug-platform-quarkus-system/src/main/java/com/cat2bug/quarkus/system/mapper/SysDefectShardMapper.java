package com.cat2bug.quarkus.system.mapper;

import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import com.cat2bug.quarkus.system.mybatis.QuarkusMapperDataSources;
import com.cat2bug.quarkus.common.domain.SysDefectShard;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface SysDefectShardMapper {

    SysDefectShard selectSysDefectShardByDefectShardId(String defectShardId);

    SysDefectShard selectSysDefectShardByDefectIdAndMemberId(@Param("defectId") Long defectId,
                                                             @Param("memberId") Long memberId);

    List<SysDefectShard> selectSysDefectShardList(SysDefectShard sysDefectShard);

    int insertSysDefectShard(SysDefectShard sysDefectShard);

    int updateSysDefectShard(SysDefectShard sysDefectShard);

    int deleteSysDefectShardByDefectShardId(String defectShardId);

    int deleteSysDefectShardByDefectShardIds(String[] defectShardIds);
}
