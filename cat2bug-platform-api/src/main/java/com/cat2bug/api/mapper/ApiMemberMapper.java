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
     * @return  成员信息
     */
    public List<ApiMember> selectMemberByNames(@Param("projectId") Long projectId, @Param("userNames") List<String> userNames);
}
