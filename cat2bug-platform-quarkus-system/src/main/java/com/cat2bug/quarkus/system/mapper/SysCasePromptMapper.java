package com.cat2bug.quarkus.system.mapper;

import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import com.cat2bug.quarkus.system.mybatis.QuarkusMapperDataSources;
import com.cat2bug.quarkus.common.domain.SysCasePrompt;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface SysCasePromptMapper {

    long countSysCasePromptList(@Param("query") SysCasePrompt query);

    List<SysCasePrompt> selectSysCasePromptList(@Param("query") SysCasePrompt query,
                                                @Param("offset") int offset,
                                                @Param("limit") int limit);

    SysCasePrompt selectSysCasePromptByCasePromptId(@Param("casePromptId") String casePromptId);

    int insertSysCasePrompt(SysCasePrompt sysCasePrompt);

    int updateSysCasePrompt(SysCasePrompt sysCasePrompt);

    int deleteSysCasePromptByCasePromptId(@Param("casePromptId") String casePromptId);

    int deleteSysCasePromptByCasePromptIds(@Param("casePromptIds") String[] casePromptIds);
}
