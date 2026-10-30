package com.cat2bug.system.mapper;

import com.cat2bug.common.domain.SysDefectPlanBaseline;
import com.cat2bug.common.domain.SysDefectPlanBaselineItem;
import com.cat2bug.system.mybatis.QuarkusMapperDataSources;
import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface SysDefectPlanBaselineMapper {

    SysDefectPlanBaseline selectByProjectId(@Param("projectId") Long projectId);

    int insertBaseline(SysDefectPlanBaseline row);

    int updateBaseline(SysDefectPlanBaseline row);

    int deleteItemsByBaselineId(@Param("baselineId") Long baselineId);

    int deleteBaselineByProjectId(@Param("projectId") Long projectId);

    int insertItem(SysDefectPlanBaselineItem item);

    List<SysDefectPlanBaselineItem> selectItemsByBaselineId(@Param("baselineId") Long baselineId);

    List<SysDefectPlanBaselineItem> selectItemsByBaselineAndDefectIds(@Param("baselineId") Long baselineId,
                                                                     @Param("defectIds") List<Long> defectIds);

    /** 项目内已排期缺陷（用于冻结基线）。 */
    List<SysDefectPlanBaselineItem> selectScheduledPlansByProjectId(@Param("projectId") Long projectId);
}
