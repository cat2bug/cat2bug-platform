package com.cat2bug.system.mapper;

import com.cat2bug.common.domain.SysScreenSize;
import com.cat2bug.system.mybatis.QuarkusMapperDataSources;
import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface SysScreenSizeMapper {

    SysScreenSize selectSysScreenSizeByScreenSizeId(@Param("screenSizeId") Long screenSizeId);

    long countSysScreenSizeList(@Param("query") SysScreenSize query);

    List<SysScreenSize> selectSysScreenSizeList(@Param("query") SysScreenSize query,
                                                @Param("offset") int offset,
                                                @Param("limit") int limit);

    List<SysScreenSize> selectSysScreenSizeListAll(@Param("query") SysScreenSize query);

    int insertSysScreenSize(SysScreenSize sysScreenSize);

    int updateSysScreenSize(SysScreenSize sysScreenSize);

    int deleteSysScreenSizeByScreenSizeId(@Param("screenSizeId") Long screenSizeId);

    int deleteSysScreenSizeByScreenSizeIds(@Param("array") Long[] screenSizeIds);
}
