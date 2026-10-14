package com.cat2bug.system.mapper;

import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import com.cat2bug.system.mybatis.QuarkusMapperDataSources;
import com.cat2bug.common.domain.SysNotice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface SysNoticeMapper {

    List<Map<String, Object>> noticeGroupStatistics(@Param("receiverId") Long receiverId);

    long countNoticeList(@Param("notice") SysNotice notice);

    List<SysNotice> selectNoticeList(@Param("notice") SysNotice notice,
                                     @Param("offset") int offset,
                                     @Param("limit") int limit);

    SysNotice selectNoticeById(@Param("noticeId") String noticeId);

    int insertNotice(SysNotice notice);

    int insertReceiverNotice(SysNotice notice);

    int updateNotice(SysNotice notice);

    int deleteNoticeByIds(@Param("noticeIds") String[] noticeIds);
}
