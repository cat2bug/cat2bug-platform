package com.cat2bug.system.mapper;

import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import com.cat2bug.system.mybatis.QuarkusMapperDataSources;
import com.cat2bug.common.domain.SysDictType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface SysDictTypeMapper {

    List<SysDictType> selectDictTypeList(@Param("query") SysDictType query);

    List<SysDictType> selectDictTypeAll();

    SysDictType selectDictTypeById(@Param("dictId") Long dictId);

    SysDictType selectDictTypeByType(@Param("dictType") String dictType);

    SysDictType checkDictTypeUnique(@Param("dictType") String dictType);

    long countDictTypeList(@Param("query") SysDictType query);

    int deleteDictTypeById(@Param("dictId") Long dictId);

    int insertDictType(SysDictType dictType);

    int updateDictType(SysDictType dictType);
}
