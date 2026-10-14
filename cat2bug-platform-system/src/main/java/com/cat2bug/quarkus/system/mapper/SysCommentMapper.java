package com.cat2bug.quarkus.system.mapper;

import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import com.cat2bug.quarkus.system.mybatis.QuarkusMapperDataSources;
import com.cat2bug.quarkus.common.domain.SysComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface SysCommentMapper {

    long countSysCommentList(@Param("comment") SysComment comment);

    List<SysComment> selectSysCommentList(@Param("comment") SysComment comment,
                                          @Param("offset") int offset,
                                          @Param("limit") int limit);

    SysComment selectSysCommentByCommentId(@Param("commentId") Long commentId);

    int insertSysComment(SysComment comment);

    int updateSysComment(SysComment comment);

    int deleteSysCommentByCommentId(@Param("commentId") Long commentId);

    int deleteSysCommentByCommentIds(@Param("commentIds") Long[] commentIds);

    int deleteSysCommentByParentId(@Param("parentId") Long parentId);
}
