package com.cat2bug.quarkus.system.mapper;

import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import com.cat2bug.quarkus.system.mybatis.QuarkusMapperDataSources;
import com.cat2bug.quarkus.common.domain.SysTempFile;
import org.apache.ibatis.annotations.Mapper;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface SysTempFileMapper {

    int insertSysTempFile(SysTempFile sysTempFile);
}
