package com.cat2bug.system.mapper;

import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import com.cat2bug.system.mybatis.QuarkusMapperDataSources;
import com.cat2bug.common.domain.SysUser;
import com.cat2bug.common.domain.SysUserProject;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface SysUserProjectMapper {

    long countSysUserListByProjectId(@Param("projectId") Long projectId, @Param("search") String search);

    List<SysUser> selectSysUserListByProjectId(@Param("projectId") Long projectId,
                                               @Param("search") String search,
                                               @Param("offset") int offset,
                                               @Param("limit") int limit);

    long countNotSysUserListByProjectId(@Param("projectId") Long projectId, @Param("search") String search);

    List<SysUser> selectNotSysUserListByProjectId(@Param("projectId") Long projectId,
                                                  @Param("search") String search,
                                                  @Param("offset") int offset,
                                                  @Param("limit") int limit);

    int insertSysUserProject(SysUserProject sysUserProject);

    int updateSysUserProjectByUserIdAndProjectId(SysUserProject sysUserProject);

    int deleteSysUserProjectByProjectIdAndUserId(@Param("projectId") Long projectId,
                                                 @Param("userId") Long userId);

    List<SysUserProject> selectSysUserProjectList(SysUserProject sysUserProject);
}
