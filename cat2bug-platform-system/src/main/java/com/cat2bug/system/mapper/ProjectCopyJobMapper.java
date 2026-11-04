package com.cat2bug.system.mapper;

import com.cat2bug.common.domain.ProjectCopyJob;
import com.cat2bug.system.mybatis.QuarkusMapperDataSources;
import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface ProjectCopyJobMapper {

    int insert(ProjectCopyJob row);

    int update(ProjectCopyJob row);

    ProjectCopyJob selectByJobId(@Param("jobId") String jobId);

    /** 当前用户在该项目上是否为创建人(11)或管理员(6)。 */
    int countCreatorOrAdmin(@Param("userId") Long userId, @Param("projectId") Long projectId);

    /** 复制 AI 账号但密钥置空（列非空，写入空串）。 */
    int insertAiAccountBlankKey(@Param("aiUrl") String aiUrl,
                                @Param("modelName") String modelName,
                                @Param("maxCompletionTokens") Long maxCompletionTokens,
                                @Param("createBy") Long createBy,
                                @Param("projectId") Long projectId,
                                @Param("accountName") String accountName);

    /** 复制外部 MCP，凭证列置空。表不存在时由调用方捕获。 */
    int copyMcpServers(@Param("sourceProjectId") Long sourceProjectId,
                       @Param("targetProjectId") Long targetProjectId);

    String selectGitKind(@Param("repositoryId") Long repositoryId);

    int updateGitKind(@Param("repositoryId") Long repositoryId, @Param("kind") String kind);
}
