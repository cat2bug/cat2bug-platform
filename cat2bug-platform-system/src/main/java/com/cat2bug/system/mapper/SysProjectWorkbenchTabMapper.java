package com.cat2bug.system.mapper;

import com.cat2bug.common.domain.SysProjectWorkbenchTab;
import com.cat2bug.system.mybatis.QuarkusMapperDataSources;
import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface SysProjectWorkbenchTabMapper {

    List<SysProjectWorkbenchTab> selectVisibleTabs(@Param("projectId") Long projectId,
                                                   @Param("userId") Long userId);

    SysProjectWorkbenchTab selectById(@Param("tabId") Long tabId);

    int countSharedByProject(@Param("projectId") Long projectId);

    int insert(SysProjectWorkbenchTab row);

    int updateMetaAndConfig(SysProjectWorkbenchTab row);

    int updateSort(@Param("list") List<SysProjectWorkbenchTab> list);

    int deleteById(@Param("tabId") Long tabId);
}
