package com.cat2bug.system.mapper;

import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import com.cat2bug.system.mybatis.QuarkusMapperDataSources;
import com.cat2bug.common.domain.SysUserStatisticTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface SysUserStatisticTemplateMapper {

    List<SysUserStatisticTemplate> selectSysUserStatisticTemplateList(
            @Param("query") SysUserStatisticTemplate query,
            @Param("offset") int offset,
            @Param("limit") int limit);

    long countSysUserStatisticTemplateList(@Param("query") SysUserStatisticTemplate query);

    SysUserStatisticTemplate selectSysUserStatisticTemplateByStatisticTemplateId(Long statisticTemplateId);

    int insertSysUserStatisticTemplate(SysUserStatisticTemplate template);

    int updateSysUserStatisticTemplate(SysUserStatisticTemplate template);

    int deleteSysUserStatisticTemplateByStatisticTemplateIds(@Param("array") Long[] statisticTemplateIds);
}
