package com.cat2bug.system.mapper;

import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import com.cat2bug.system.mybatis.QuarkusMapperDataSources;
import com.cat2bug.common.domain.SysDocument;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface SysDocumentMapper {

    long countSysDocumentList(@Param("query") SysDocument query);

    List<SysDocument> selectSysDocumentList(@Param("query") SysDocument query,
                                            @Param("offset") int offset,
                                            @Param("limit") int limit);

    SysDocument selectSysDocumentByDocId(@Param("docId") Long docId);

    int insertSysDocument(SysDocument sysDocument);

    int updateSysDocument(SysDocument sysDocument);

    int deleteSysDocumentByDocId(@Param("docId") Long docId);

    int deleteSysDocumentByDocIds(@Param("docIds") Long[] docIds);
}
