package com.cat2bug.system.mapper;

import com.cat2bug.system.mybatis.QuarkusMapperDataSources;
import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface SysMcpIdempotencyMapper
{
    String selectResultId(@Param("userId") Long userId, @Param("tool") String tool, @Param("key") String key,
                          @Param("after") Date after);

    int insert(@Param("userId") Long userId, @Param("tool") String tool, @Param("key") String key,
               @Param("resultId") String resultId, @Param("createTime") Date createTime);

    int deleteBefore(@Param("before") Date before);

    int deleteOne(@Param("userId") Long userId, @Param("tool") String tool, @Param("key") String key);
}
