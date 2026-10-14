package com.cat2bug.quarkus.system.mapper;

import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import com.cat2bug.quarkus.system.mybatis.QuarkusMapperDataSources;
import com.cat2bug.quarkus.common.domain.SysPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface SysPostMapper {

    long countPostList(@Param("post") SysPost post);

    List<SysPost> selectPostList(@Param("post") SysPost post,
                                 @Param("offset") int offset,
                                 @Param("limit") int limit);

    List<SysPost> selectPostListAll(@Param("post") SysPost post);

    List<SysPost> selectPostAll();

    SysPost selectPostById(@Param("postId") Long postId);

    List<Long> selectPostListByUserId(@Param("userId") Long userId);

    List<SysPost> selectPostsByUserName(@Param("userName") String userName);

    SysPost checkPostNameUnique(@Param("postName") String postName);

    SysPost checkPostCodeUnique(@Param("postCode") String postCode);

    int insertPost(SysPost post);

    int updatePost(SysPost post);

    int deletePostById(@Param("postId") Long postId);

    int deletePostByIds(@Param("array") Long[] postIds);
}
