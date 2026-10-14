package com.cat2bug.quarkus.system.mapper;

import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import com.cat2bug.quarkus.system.mybatis.QuarkusMapperDataSources;
import com.cat2bug.quarkus.common.domain.AiAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface AiAccountMapper {

    List<AiAccount> selectAiAccountList(@Param("query") AiAccount query);

    AiAccount selectAiAccountByAccountId(@Param("accountId") Long accountId);

    int insertAiAccount(AiAccount aiAccount);

    int updateAiAccount(AiAccount aiAccount);

    int deleteAiAccountByAccountId(@Param("accountId") Long accountId);

    int deleteAiAccountByAccountIds(@Param("accountIds") Long[] accountIds);
}
