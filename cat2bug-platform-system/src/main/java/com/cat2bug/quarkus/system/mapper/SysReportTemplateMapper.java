package com.cat2bug.quarkus.system.mapper;

import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import com.cat2bug.quarkus.system.mybatis.QuarkusMapperDataSources;
import com.cat2bug.quarkus.common.domain.SysReportTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface SysReportTemplateMapper {

    long countSysReportTemplateList(@Param("query") SysReportTemplate query);

    List<SysReportTemplate> selectSysReportTemplateList(@Param("query") SysReportTemplate query,
                                                        @Param("offset") int offset,
                                                        @Param("limit") int limit);

    SysReportTemplate selectSysReportTemplateByTemplateId(@Param("templateId") Long templateId);

    int insertSysReportTemplate(SysReportTemplate sysReportTemplate);

    int updateSysReportTemplate(SysReportTemplate sysReportTemplate);

    int deleteSysReportTemplateByTemplateId(@Param("templateId") Long templateId);

    int deleteSysReportTemplateByTemplateIds(@Param("templateIds") Long[] templateIds);
}
