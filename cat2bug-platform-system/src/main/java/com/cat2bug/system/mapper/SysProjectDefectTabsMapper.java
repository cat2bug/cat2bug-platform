package com.cat2bug.system.mapper;

import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import com.cat2bug.system.mybatis.QuarkusMapperDataSources;
import com.cat2bug.common.domain.SysProjectDefectTabs;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface SysProjectDefectTabsMapper {

    long countSysProjectDefectTabsList(@Param("tabs") SysProjectDefectTabs tabs);

    List<SysProjectDefectTabs> selectSysProjectDefectTabsList(@Param("tabs") SysProjectDefectTabs tabs,
                                                                @Param("offset") int offset,
                                                                @Param("limit") int limit);

    List<SysProjectDefectTabs> selectSysProjectDefectTabsListAll(@Param("tabs") SysProjectDefectTabs tabs);

    SysProjectDefectTabs selectSysProjectDefectTabsByTabId(@Param("tabId") Long tabId);

    int insertSysProjectDefectTabs(SysProjectDefectTabs tabs);

    int updateSysProjectDefectTabs(SysProjectDefectTabs tabs);

    int updateSort(@Param("list") List<SysProjectDefectTabs> list);

    int deleteSysProjectDefectTabsByTabId(@Param("tabId") Long tabId);

    int deleteSysProjectDefectTabsByTabIds(@Param("tabIds") Long[] tabIds);
}
