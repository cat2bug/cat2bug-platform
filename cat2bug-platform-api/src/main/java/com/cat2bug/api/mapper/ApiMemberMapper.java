package com.cat2bug.api.mapper;

import com.cat2bug.api.domain.ApiMember;
import com.cat2bug.common.core.domain.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 成员
 */
@Mapper
public interface ApiMemberMapper {
    /**
     * 查找成员
     * @param projectId 项目id
     * @param userNames  成员登陆名
     * @return  成员集合
     */
    public List<ApiMember> selectMemberByNames(@Param("projectId") Long projectId, @Param("userNames") List<String> userNames);

    /**
     * 查询成员集合
     * @param projectId 项目ID
     * @param apiMember 成员参数
     * @return  成员集合
     */
    public List<ApiMember> selectMemberList(@Param("projectId") Long projectId, @Param("apiMember") ApiMember apiMember);
}
