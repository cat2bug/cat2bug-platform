package com.cat2bug.quarkus.system.mapper;

import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import com.cat2bug.quarkus.system.mybatis.QuarkusMapperDataSources;
import com.cat2bug.quarkus.common.domain.SysDictData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface SysDictDataMapper {

    List<SysDictData> selectDictDataList(@Param("query") SysDictData query);

    List<SysDictData> selectDictDataByType(@Param("dictType") String dictType);

    SysDictData selectDictDataById(@Param("dictCode") Long dictCode);

    long countDictDataList(@Param("query") SysDictData query);

    int insertDictData(SysDictData dictData);

    int updateDictData(SysDictData dictData);

    int deleteDictDataById(@Param("dictCode") Long dictCode);

    int countDictDataByType(@Param("dictType") String dictType);

    int updateDictDataType(@Param("oldDictType") String oldDictType, @Param("newDictType") String newDictType);
}
