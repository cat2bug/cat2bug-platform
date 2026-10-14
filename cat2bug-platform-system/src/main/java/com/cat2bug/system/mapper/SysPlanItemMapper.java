package com.cat2bug.system.mapper;

import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import com.cat2bug.system.mybatis.QuarkusMapperDataSources;
import com.cat2bug.common.domain.SysCase;
import com.cat2bug.common.domain.SysPlanItem;
import com.cat2bug.common.domain.SysPlanItemModule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface SysPlanItemMapper {

    long countSysPlanItemList(@Param("query") SysPlanItem sysPlanItem);

    List<SysPlanItem> selectSysPlanItemList(@Param("query") SysPlanItem sysPlanItem,
                                            @Param("offset") int offset,
                                            @Param("limit") int limit);

    List<SysPlanItem> selectAllSysPlanItemList(@Param("query") SysPlanItem sysPlanItem);

    SysPlanItem selectSysPlanItemByPlanItemId(@Param("planItemId") String planItemId);

    SysPlanItem selectPrevSysPlanItem(@Param("query") SysPlanItem sysPlanItem);

    SysPlanItem selectNextSysPlanItem(@Param("query") SysPlanItem sysPlanItem);

    long countCaseList(@Param("query") SysCase sysCase);

    List<SysCase> selectCaseList(@Param("query") SysCase sysCase,
                                 @Param("offset") int offset,
                                 @Param("limit") int limit);

    List<SysPlanItemModule> selectSysModuleList(SysPlanItemModule sysModule);

    int insertSysPlanItem(SysPlanItem sysPlanItem);

    int updateSysPlanItem(SysPlanItem sysPlanItem);

    int batchUpdateSysPlanItem(SysPlanItem sysPlanItem);

    int deleteSysPlanItemByPlanItemId(@Param("planItemId") String planItemId);

    int deleteSysPlanItemByPlanItemIds(@Param("array") String[] planItemIds);
}
