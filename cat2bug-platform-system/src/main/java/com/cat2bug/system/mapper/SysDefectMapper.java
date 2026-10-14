package com.cat2bug.system.mapper;

import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import com.cat2bug.system.mybatis.QuarkusMapperDataSources;
import com.cat2bug.common.domain.SysDefect;
import com.cat2bug.common.domain.SysVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface SysDefectMapper {

    long countSysDefectList(@Param("defect") SysDefect defect);

    List<SysDefect> selectSysDefectList(@Param("defect") SysDefect defect,
                                        @Param("offset") int offset,
                                        @Param("limit") int limit);

    List<SysDefect> selectAllSysDefectList(@Param("defect") SysDefect defect);

    SysDefect selectSysDefectByDefectId(@Param("defectId") Long defectId);

    SysDefect selectByProjectIdAndProjectNum(@Param("projectId") Long projectId,
                                             @Param("projectNum") Long projectNum);

    Long getProjectDefectMaxNum(@Param("projectId") Long projectId);

    int insertSysDefect(SysDefect defect);

    int batchInsertSysDefect(@Param("defectList") List<SysDefect> defectList);

    int updateSysDefect(SysDefect defect);

    int deleteSysDefectByDefectId(@Param("defectId") Long defectId);

    int restoreSysDefectByDefectId(SysDefect defect);

    List<SysVersion> selectVersionList(@Param("projectId") Long projectId);
}
