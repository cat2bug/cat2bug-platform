package com.cat2bug.system.mapper;

import com.cat2bug.system.domain.SysDefectLine;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysDashboardMapper {
    public List<SysDefectLine> defectLine(@Param("projectId") Long projectId, @Param("timeType") String timeType);
}
