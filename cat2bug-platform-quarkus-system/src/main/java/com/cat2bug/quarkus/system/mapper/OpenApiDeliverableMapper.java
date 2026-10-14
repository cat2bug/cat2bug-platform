package com.cat2bug.quarkus.system.mapper;

import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import com.cat2bug.quarkus.system.mybatis.QuarkusMapperDataSources;
import com.cat2bug.quarkus.common.domain.open.ApiDeliverable;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface OpenApiDeliverableMapper {

    List<ApiDeliverable> selectApiDeliverableList(@Param("projectId") Long projectId,
                                                  @Param("apiDeliverable") ApiDeliverable apiDeliverable);

    List<ApiDeliverable> selectApiDeliverablePathList(@Param("projectId") Long projectId);

    List<ApiDeliverable> selectApiDeliverableTreeFlat(@Param("projectId") Long projectId);

    int countModuleChildren(@Param("projectId") Long projectId, @Param("moduleId") Long moduleId);
}
