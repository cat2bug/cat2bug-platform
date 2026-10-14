package com.cat2bug.quarkus.system.mapper;

import org.apache.ibatis.annotations.Mapper;
import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import com.cat2bug.quarkus.system.mybatis.QuarkusMapperDataSources;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.cat2bug.quarkus.common.domain.SysGitRepositoryMember;

/**
 * 仓库成员 ACL Mapper
 */
@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface SysGitRepositoryMemberMapper
{
    /**
     * 按仓库列出成员（含用户展示字段）
     */
    List<SysGitRepositoryMember> selectMembersByRepositoryId(@Param("repositoryId") Long repositoryId);

    /**
     * 按仓库 + 用户查询 ACL 行
     */
    SysGitRepositoryMember selectByRepositoryIdAndUserId(@Param("repositoryId") Long repositoryId,
            @Param("userId") Long userId);

    /**
     * 统计仓库下 admin 角色行数
     */
    int countAdminsByRepositoryId(@Param("repositoryId") Long repositoryId);

    /**
     * 用户在指定项目下是否有任意仓 ACL（用于路径解析：非项目成员但已是仓成员）
     */
    int countByProjectIdAndUserId(@Param("projectId") Long projectId, @Param("userId") Long userId);

    /**
     * 项目下各仓库 ACL 成员去重预览（同用户多仓取最高角色；仅启用项目内的仓库）
     */
    List<SysGitRepositoryMember> selectDistinctMembersByProjectId(@Param("projectId") Long projectId);

    /**
     * 新增 ACL 行
     */
    int insertSysGitRepositoryMember(SysGitRepositoryMember member);

    /**
     * 更新角色
     */
    int updateMemberRole(SysGitRepositoryMember member);

    /**
     * 删除 ACL 行
     */
    int deleteByRepositoryIdAndUserId(@Param("repositoryId") Long repositoryId, @Param("userId") Long userId);

    /**
     * 按仓库删除全部 ACL（仓库删除时级联清理可选）
     */
    int deleteByRepositoryId(@Param("repositoryId") Long repositoryId);
}
