package com.cat2bug.quarkus.system.mapper;

import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import com.cat2bug.quarkus.system.mybatis.QuarkusMapperDataSources;
import com.cat2bug.quarkus.common.domain.SysUserPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface SysUserPostMapper {

    int deleteUserPostByUserId(@Param("userId") Long userId);

    int countUserPostById(@Param("postId") Long postId);

    int deleteUserPost(@Param("array") Long[] userIds);

    int batchUserPost(@Param("list") List<SysUserPost> list);
}
